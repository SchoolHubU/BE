package com.shu.backend.domain.chatroom.service;


import com.shu.backend.domain.chatmessage.repository.ChatMessageRepository;
import com.shu.backend.domain.chatroom.dto.ChatRoomDTO;
import com.shu.backend.domain.chatroom.entity.ChatRoom;
import com.shu.backend.domain.chatroom.exception.ChatRoomException;
import com.shu.backend.domain.chatroom.exception.status.ChatRoomErrorStatus;
import com.shu.backend.domain.chatroom.repository.ChatRoomRepository;
import com.shu.backend.domain.chatroomuser.entity.ChatRoomUser;
import com.shu.backend.domain.chatroomuser.repository.ChatRoomUserRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatRoomDTO.CreateDmResponse findOrCreateDm(Long myId, Long otherId) {

        long u1 = Math.min(myId, otherId);
        long u2 = Math.max(myId, otherId);

        ChatRoom room = chatRoomRepository.findByUser1IdAndUser2Id(u1, u2)
                .orElseGet(() -> {
                    // 정규화된 u1,u2로 저장
                    ChatRoom created = chatRoomRepository.save(ChatRoom.ofDm(u1, u2));

                    User me = userRepository.getReferenceById(myId);
                    User other = userRepository.getReferenceById(otherId);

                    // 둘 다 hidden=true로 시작 (목록에 안 뜸)
                    chatRoomUserRepository.save(ChatRoomUser.createHidden(created, me));
                    chatRoomUserRepository.save(ChatRoomUser.createHidden(created, other));

                    return created;
                });

        return ChatRoomDTO.CreateDmResponse.builder()
                .roomId(room.getId())
                .otherUserId(otherId)
                .lastMessageAt(room.getLastMessageAt())
                .displayName(room.getDisplayName())
                .build();
    }

    //채팅방 조회
    @Transactional(readOnly = true)
    public ChatRoomDTO.RoomListResponse getMyRooms(Long myId) {

        List<ChatRoomUser> mine =
                new java.util.ArrayList<>(chatRoomUserRepository.findByUserIdAndHiddenFalseAndBlockedAtIsNull(myId));

        mine.sort(Comparator.comparing(
                cru -> cru.getChatRoom().getLastMessageAt(),
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        // 방 목록 DTO 변환
        List<ChatRoomDTO.RoomListItem> items = mine.stream().map(cru -> {
            ChatRoom room = cru.getChatRoom();

            // 1:1 DM에서 상대방 ID 계산
            Long otherId = room.getUser1Id().equals(myId) ? room.getUser2Id() : room.getUser1Id();

            // 마지막 메시지 미리보기(현재는 MVP로 문자열만 처리)
            String preview = (room.getLastMessageId() == null) ? "" : "(마지막 메시지)";

            // 미읽음 여부(MVP: lastMessageId와 lastReadMessageId 비교로 0/1만 제공)
            long unread = 0L;
            if (room.getLastMessageId() != null) {
                Long lastRead = cru.getLastReadMessageId();
                unread = (lastRead == null) ? 1L : (room.getLastMessageId() > lastRead ? 1L : 0L);
            }

            return ChatRoomDTO.RoomListItem.builder()
                    .roomId(room.getId())
                    .otherUserId(otherId)
                    .lastPreview(preview)
                    .lastMessageAt(room.getLastMessageAt())
                    .unreadCount(unread)
                    .displayName(room.getDisplayName())

                    .build();
        }).toList();

        // 최종 응답 DTO 반환
        return ChatRoomDTO.RoomListResponse.builder()
                .rooms(items)
                .build();
    }

    public void leave(Long myId, Long roomId) {

        // 방 참여자 정보 조회(참여자가 아니면 예외)
        ChatRoomUser cru = chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorStatus.NOT_ROOM_MEMBER));

        // 나가기 처리(leftAt/hidden 등은 엔티티 메서드에서 처리)
        cru.leave();
    }

    public void block(Long myId, Long roomId) {

        // 방 참여자 정보 조회(참여자가 아니면 예외)
        ChatRoomUser cru = chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorStatus.NOT_ROOM_MEMBER));

        // 차단 처리(blockedAt/hidden 등은 엔티티 메서드에서 처리)
        cru.block();
    }
}