package com.shu.backend.domain.chatroom.service;

import com.shu.backend.domain.board.service.BoardAccessPolicy;
import com.shu.backend.domain.boardprofile.entity.BoardDisplayProfile;
import com.shu.backend.domain.boardprofile.service.BoardDisplayProfileService;
import com.shu.backend.domain.chatmessage.entity.ChatMessage;
import com.shu.backend.domain.chatmessage.repository.ChatMessageRepository;
import com.shu.backend.domain.chatroom.dto.ChatRoomDTO;
import com.shu.backend.domain.chatroom.entity.ChatRoom;
import com.shu.backend.domain.chatroom.exception.ChatRoomException;
import com.shu.backend.domain.chatroom.exception.status.ChatRoomErrorStatus;
import com.shu.backend.domain.chatroom.repository.ChatRoomRepository;
import com.shu.backend.domain.chatroomuser.entity.ChatRoomUser;
import com.shu.backend.domain.chatroomuser.repository.ChatRoomUserRepository;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.report.dto.ReportDTO;
import com.shu.backend.domain.report.enums.ReportReason;
import com.shu.backend.domain.report.enums.TargetType;
import com.shu.backend.domain.report.service.ReportService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.user.support.UserDisplay;
import com.shu.backend.global.util.HtmlText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ReportService reportService;
    private final BoardAccessPolicy boardAccessPolicy;
    private final PostRepository postRepository;
    private final BoardDisplayProfileService boardDisplayProfileService;

    public ChatRoomDTO.CreateDmResponse findOrCreateDm(Long myId, Long otherId, Long sourcePostId, String roomTitle) {
        User me = boardAccessPolicy.requireVerifiedActiveUserWithSchool(myId);
        User other = boardAccessPolicy.requireVerifiedActiveUserWithSchool(otherId);
        if (UserDisplay.isDeleted(other)) {
            throw new ChatRoomException(ChatRoomErrorStatus.TARGET_USER_DELETED);
        }
        assertSameSchool(me, other);
        if (sourcePostId == null) {
            throw new ChatRoomException(ChatRoomErrorStatus.CHAT_ROOM_NOT_FOUND);
        }

        Post sourcePost = postRepository.findDetailById(sourcePostId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorStatus.CHAT_ROOM_NOT_FOUND));
        String title = normalizeRoomTitle(sourcePost.getTitle(), roomTitle);

        long u1 = Math.min(myId, otherId);
        long u2 = Math.max(myId, otherId);

        ChatRoom room = chatRoomRepository.findByUser1IdAndUser2IdAndSourcePostId(u1, u2, sourcePostId)
                .orElseGet(() -> {
                    ChatRoom created = chatRoomRepository.save(ChatRoom.ofDm(u1, u2, sourcePostId, title));
                    chatRoomUserRepository.save(ChatRoomUser.createHidden(created, me));
                    chatRoomUserRepository.save(ChatRoomUser.createHidden(created, other));
                    return created;
                });

        ChatRoomUser myCru = chatRoomUserRepository.findByChatRoomIdAndUserId(room.getId(), myId)
                .orElse(null);
        ChatRoomUser otherCru = chatRoomUserRepository.findByChatRoomIdAndUserId(room.getId(), otherId)
                .orElse(null);
        boolean blockedByMe = myCru != null && myCru.isBlocked();
        boolean blockedByOther = otherCru != null && otherCru.isBlocked();
        boolean blocked = blockedByMe || blockedByOther;
        CounterpartProfile counterpart = counterpartProfile(other, sourcePost);

        return ChatRoomDTO.CreateDmResponse.builder()
                .roomId(room.getId())
                .otherUserId(otherId)
                .roomTitle(room.getDisplayName())
                .counterpartDisplayName(counterpart.displayName())
                .counterpartProfileImageUrl(counterpart.profileImageUrl())
                .displayName(room.getDisplayName())
                .lastMessageAt(room.getLastMessageAt())
                .blocked(blocked)
                .blockedByMe(blockedByMe)
                .blockedByOther(blockedByOther)
                .otherUserDeleted(false)
                .canSendMessage(!blocked)
                .canReport(true)
                .canBlock(true)
                .build();
    }

    @Transactional(readOnly = true)
    public ChatRoomDTO.RoomListResponse getMyRooms(Long myId) {
        boardAccessPolicy.requireVerifiedActiveUserWithSchool(myId);

        List<ChatRoomUser> mine =
                new java.util.ArrayList<>(chatRoomUserRepository.findByUserIdAndHiddenFalse(myId));

        mine.sort(Comparator.comparing(
                cru -> cru.getChatRoom().getLastMessageAt(),
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        List<Long> lastMsgIds = mine.stream()
                .map(cru -> cru.getChatRoom().getLastMessageId())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, ChatMessage> lastMsgMap = chatMessageRepository.findAllById(lastMsgIds)
                .stream()
                .collect(Collectors.toMap(ChatMessage::getId, Function.identity()));

        List<Long> roomIds = mine.stream()
                .map(cru -> cru.getChatRoom().getId())
                .toList();

        Map<String, ChatRoomUser> chatRoomUserMap = roomIds.isEmpty()
                ? Map.of()
                : chatRoomUserRepository.findByChatRoomIdIn(roomIds)
                .stream()
                .collect(Collectors.toMap(
                        cru -> cru.getChatRoom().getId() + ":" + cru.getUser().getId(),
                        Function.identity()
                ));

        Map<Long, Long> unreadMap = roomIds.isEmpty()
                ? Map.of()
                : chatMessageRepository.countUnreadByRoomIds(roomIds, myId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        List<Long> otherIds = mine.stream()
                .map(cru -> otherUserId(cru.getChatRoom(), myId))
                .distinct()
                .toList();
        Map<Long, User> otherUserMap = otherIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(otherIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Long> sourcePostIds = mine.stream()
                .map(cru -> cru.getChatRoom().getSourcePostId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Post> sourcePostMap = sourcePostIds.isEmpty()
                ? Map.of()
                : postRepository.findByIdIn(sourcePostIds).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        List<ChatRoomDTO.RoomListItem> items = mine.stream().map(cru -> {
            ChatRoom room = cru.getChatRoom();
            Long otherId = otherUserId(room, myId);
            User other = otherUserMap.get(otherId);
            Post sourcePost = sourcePostMap.get(room.getSourcePostId());
            boolean otherDeleted = UserDisplay.isDeleted(other);
            boolean blockedByMe = cru.isBlocked();
            ChatRoomUser otherCru = chatRoomUserMap.get(room.getId() + ":" + otherId);
            boolean blockedByOther = otherCru != null && otherCru.isBlocked();
            boolean blocked = blockedByMe || blockedByOther;

            String preview = "";
            if (room.getLastMessageId() != null) {
                ChatMessage lastMsg = lastMsgMap.get(room.getLastMessageId());
                if (lastMsg != null) {
                    preview = lastMsg.getType() == ChatMessage.MessageType.IMAGE
                            ? "[사진]"
                            : (lastMsg.getContent() != null ? lastMsg.getContent() : "");
                }
            }

            long unread = unreadMap.getOrDefault(room.getId(), 0L);
            String title = normalizeRoomTitle(sourcePost != null ? sourcePost.getTitle() : null, room.getDisplayName());
            CounterpartProfile counterpart = otherDeleted
                    ? new CounterpartProfile(UserDisplay.DELETED_USER_NAME, null)
                    : counterpartProfile(other, sourcePost);

            return ChatRoomDTO.RoomListItem.builder()
                    .roomId(room.getId())
                    .otherUserId(otherId)
                    .lastPreview(preview)
                    .lastMessageAt(room.getLastMessageAt())
                    .unreadCount(unread)
                    .roomTitle(title)
                    .counterpartDisplayName(counterpart.displayName())
                    .counterpartProfileImageUrl(counterpart.profileImageUrl())
                    .displayName(title)
                    .blocked(blocked)
                    .blockedByMe(blockedByMe)
                    .blockedByOther(blockedByOther)
                    .otherUserDeleted(otherDeleted)
                    .canSendMessage(!otherDeleted && !blocked)
                    .canReport(!otherDeleted)
                    .canBlock(!otherDeleted)
                    .build();
        }).toList();

        return ChatRoomDTO.RoomListResponse.builder()
                .rooms(items)
                .build();
    }

    public void leave(Long myId, Long roomId) {
        boardAccessPolicy.requireVerifiedActiveUserWithSchool(myId);

        ChatRoomUser cru = chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorStatus.NOT_ROOM_MEMBER));
        cru.leave();
    }

    public void block(Long myId, Long roomId) {
        boardAccessPolicy.requireVerifiedActiveUserWithSchool(myId);

        ChatRoomUser cru = chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorStatus.NOT_ROOM_MEMBER));
        assertOtherUserActive(myId, cru.getChatRoom());
        cru.block();
    }

    public void unblock(Long myId, Long roomId) {
        boardAccessPolicy.requireVerifiedActiveUserWithSchool(myId);

        ChatRoomUser cru = chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorStatus.NOT_ROOM_MEMBER));
        cru.unblock();
    }

    public void report(Long myId, Long roomId, ReportReason reason, String detail) {
        boardAccessPolicy.requireVerifiedActiveUserWithSchool(myId);

        ChatRoomUser cru = chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorStatus.NOT_ROOM_MEMBER));
        Long otherId = otherUserId(cru.getChatRoom(), myId);
        assertOtherUserActive(myId, cru.getChatRoom());

        reportService.create(myId, new ReportDTO.CreateRequest(
                TargetType.USER,
                otherId,
                reason,
                detail
        ));
        cru.block();
        log.info("Chat room report created and room blocked - reporterId={}, roomId={}, reportedUserId={}", myId, roomId, otherId);
    }

    @Transactional(readOnly = true)
    public List<Long> getParticipantIds(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorStatus.CHAT_ROOM_NOT_FOUND));
        return List.of(room.getUser1Id(), room.getUser2Id());
    }

    private Long otherUserId(ChatRoom room, Long myId) {
        return room.getUser1Id().equals(myId) ? room.getUser2Id() : room.getUser1Id();
    }

    private void assertSameSchool(User me, User other) {
        if (me.getSchool() == null
                || other.getSchool() == null
                || !me.getSchool().getId().equals(other.getSchool().getId())) {
            throw new ChatRoomException(ChatRoomErrorStatus.SCHOOL_MISMATCH);
        }
    }

    private void assertOtherUserActive(Long myId, ChatRoom room) {
        Long otherId = otherUserId(room, myId);
        User other = userRepository.findById(otherId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));
        if (UserDisplay.isDeleted(other)) {
            throw new ChatRoomException(ChatRoomErrorStatus.TARGET_USER_DELETED);
        }
    }

    private String normalizeRoomTitle(String postTitle, String fallback) {
        if (postTitle != null && !postTitle.isBlank()) {
            return HtmlText.unescape(postTitle);
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return "채팅방";
    }

    private CounterpartProfile counterpartProfile(User user, Post sourcePost) {
        if (user == null || UserDisplay.isDeleted(user)) {
            return new CounterpartProfile(UserDisplay.DELETED_USER_NAME, null);
        }
        if (user.getRole() == UserRole.ADMIN) {
            return new CounterpartProfile(UserDisplay.nicknameOrDeleted(user), user.getProfileImageUrl());
        }
        if (sourcePost == null || sourcePost.getBoard() == null) {
            return new CounterpartProfile(UserDisplay.nicknameOrDeleted(user), user.getProfileImageUrl());
        }
        BoardDisplayProfile profile = boardDisplayProfileService.getOrCreate(user, sourcePost.getBoard());
        return new CounterpartProfile(
                profile.getDisplayName(),
                boardDisplayProfileService.toReadUrl(profile.getProfileImageUrl())
        );
    }

    private record CounterpartProfile(String displayName, String profileImageUrl) {
    }
}
