package com.shu.backend.domain.chatmessage.service;

import com.shu.backend.domain.chatmessage.dto.ChatMessageDTO;
import com.shu.backend.domain.chatmessage.entity.ChatMessage;
import com.shu.backend.domain.chatmessage.exception.ChatMessageException;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageErrorStatus;
import com.shu.backend.domain.chatmessage.repository.ChatMessageRepository;
import com.shu.backend.domain.chatroom.entity.ChatRoom;
import com.shu.backend.domain.chatroom.repository.ChatRoomRepository;
import com.shu.backend.domain.chatroomuser.entity.ChatRoomUser;
import com.shu.backend.domain.chatroomuser.repository.ChatRoomUserRepository;
import com.shu.backend.domain.media.entity.Media;
import com.shu.backend.domain.media.enums.MediaType;
import com.shu.backend.domain.media.repository.MediaRepository;
import com.shu.backend.domain.notification.enums.NotificationTargetType;
import com.shu.backend.domain.notification.enums.NotificationType;
import com.shu.backend.domain.notification.service.NotificationService;
import com.shu.backend.domain.push.service.PushService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final ChatMessageRepository chatMessageRepository;

    private final UserRepository userRepository;
    private final MediaRepository mediaRepository;

    private final NotificationService notificationService;
    private final PushService pushService;

    @PreAuthorize("@penaltyChecker.notPenalized(#senderId)")
    public ChatMessageDTO.MessageResponse send(Long senderId, ChatMessageDTO.SendRequest req) {

        // 채팅방 조회 (없으면 예외)
        ChatRoom room = chatRoomRepository.findById(req.getRoomId())
                .orElseThrow(() -> new ChatMessageException(ChatMessageErrorStatus.CHAT_ROOM_NOT_FOUND));

        // 발신자 참여자 검증 (참여자가 아니면 예외)
        ChatRoomUser senderCru = chatRoomUserRepository.findByChatRoomIdAndUserId(room.getId(), senderId)
                .orElseThrow(() -> new ChatMessageException(ChatMessageErrorStatus.NOT_ROOM_MEMBER));

        // 1:1 DM에서 수신자 ID 계산
        Long receiverId = room.getUser1Id().equals(senderId) ? room.getUser2Id() : room.getUser1Id();

        // 수신자 참여자 정보 조회 (없으면 예외)
        ChatRoomUser receiverCru = chatRoomUserRepository.findByChatRoomIdAndUserId(room.getId(), receiverId)
                .orElseThrow(() -> new ChatMessageException(ChatMessageErrorStatus.NOT_ROOM_MEMBER));

        // 차단 상태면 메시지 전송 불가
        if (senderCru.isBlocked() || receiverCru.isBlocked()) {
            throw new ChatMessageException(ChatMessageErrorStatus.CHAT_BLOCKED);
        }

        // 수신자가 나갔거나 숨김이면, 상대가 메시지 보낼 때 자동 복귀 처리
        receiverCru.rejoinIfLeftOrHidden();
        senderCru.rejoinIfLeftOrHidden();

        // 발신자 User 레퍼런스 조회(프록시)
        User sender = userRepository.getReferenceById(senderId);

        // 요청 타입(TEXT/IMAGE) 변환 및 검증
        ChatMessage.MessageType type = mapType(req.getType());

        // IMAGE 메시지는 imageUrl 필수
        if (type == ChatMessage.MessageType.IMAGE) {
            if (req.getImageUrl() == null || req.getImageUrl().isBlank()) {
                throw new ChatMessageException(ChatMessageErrorStatus.IMAGE_URL_REQUIRED);
            }
        }

        // 메시지 엔티티 생성 (TEXT만 content 저장, IMAGE는 content 비워둠)
        ChatMessage message = ChatMessage.builder()
                .type(type)
                .content(type == ChatMessage.MessageType.TEXT ? req.getContent() : null)
                .chatRoom(room)
                .sender(sender)
                .build();

        // 메시지 저장
        ChatMessage saved = chatMessageRepository.save(message);

        // IMAGE 메시지면 Media에 연결 저장 (targetType=CHAT_MESSAGE, targetId=messageId)
        ChatMessageDTO.MediaItem mediaItem = null;
        if (type == ChatMessage.MessageType.IMAGE) {
            Media media = Media.ofChatMessage(req.getImageUrl(), saved.getId(), MediaType.IMAGE, sender);
            Media savedMedia = mediaRepository.save(media);

            mediaItem = ChatMessageDTO.MediaItem.builder()
                    .id(savedMedia.getId())
                    .url(savedMedia.getUrl())
                    .mediaType(savedMedia.getMediaType().name())
                    .build();
        }

        // 채팅방 요약 필드 갱신 (방 목록 최신순 정렬/미리보기용)
        room.updateLastMessage(saved.getId(), saved.getCreatedAt());

        String notiMsg = (type == ChatMessage.MessageType.TEXT)
                ? "새 메시지: " + summarize(req.getContent(), 20)
                : "사진을 보냈습니다.";


        //새로운 채팅 알림 생성
        Long notificationId = notificationService.create(
                NotificationType.CHAT,
                NotificationTargetType.CHAT_MSG,
                room.getId(),
                notiMsg,
                receiverId,
                senderId
        );

        //푸시 알림 전송
        if (notificationId != null) {
            try {
                pushService.sendToUser(
                        receiverId,
                        "새 채팅",
                        notiMsg,
                        Map.of(
                                "notificationId", String.valueOf(notificationId),
                                "type", NotificationType.CHAT.name(),
                                "targetType", NotificationTargetType.CHAT_MSG.name(),
                                "targetId", String.valueOf(room.getId())
                        )
                );
            } catch (Exception ignore) {
            }
        }

        // 응답 DTO 반환
        return ChatMessageDTO.MessageResponse.builder()
                .messageId(saved.getId())
                .roomId(room.getId())
                .senderId(senderId)
                .type(req.getType())
                .content(saved.getContent())
                .medias(mediaItem == null ? Collections.emptyList() : List.of(mediaItem))
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public ChatMessageDTO.MessageListResponse getMessages(Long myId, Long roomId, Long lastId) {

        chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)
                .orElseThrow(() -> new ChatMessageException(ChatMessageErrorStatus.NOT_ROOM_MEMBER));

        List<ChatMessage> list = (lastId == null)
                ? chatMessageRepository.findTop50ByChatRoomIdOrderByIdDesc(roomId)
                : chatMessageRepository.findTop50ByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, lastId);

        // messageIds 추출
        List<Long> messageIds = list.stream().map(ChatMessage::getId).toList();

        // Media 한번에 조회 후 targetId(messageId)로 그룹핑
        var mediaMap = messageIds.isEmpty()
                ? java.util.Collections.<Long, List<Media>>emptyMap()
                : mediaRepository.findByTargetTypeAndTargetIdIn(
                com.shu.backend.domain.media.enums.MediaTargetType.CHAT_MESSAGE,
                messageIds
        ).stream().collect(java.util.stream.Collectors.groupingBy(Media::getTargetId));

        List<ChatMessageDTO.MessageResponse> res = list.stream().map(m -> {
            List<ChatMessageDTO.MediaItem> mediaItems =
                    mediaMap.getOrDefault(m.getId(), java.util.Collections.emptyList())
                            .stream()
                            .map(md -> ChatMessageDTO.MediaItem.builder()
                                    .id(md.getId())
                                    .url(md.getUrl())
                                    .mediaType(md.getMediaType().name())
                                    .build())
                            .toList();

            return ChatMessageDTO.MessageResponse.builder()
                    .messageId(m.getId())
                    .roomId(roomId)
                    .senderId(m.getSender().getId())
                    .type(ChatMessageDTO.MessageType.valueOf(m.getType().name()))
                    .content(m.getContent())
                    .medias(mediaItems)
                    .createdAt(m.getCreatedAt())
                    .build();
        }).toList();

        return ChatMessageDTO.MessageListResponse.builder()
                .roomId(roomId)
                .messages(res)
                .build();
    }

    public void read(Long myId, Long roomId, Long messageId) {

        // 참여자 조회 (참여자가 아니면 예외)
        ChatRoomUser cru = chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)
                .orElseThrow(() -> new ChatMessageException(ChatMessageErrorStatus.NOT_ROOM_MEMBER));

        // 읽음 처리 (lastReadMessageId 최신화)
        cru.read(messageId);
    }

    private ChatMessage.MessageType mapType(ChatMessageDTO.MessageType type) {

        // 타입이 null이면 예외
        if (type == null) throw new ChatMessageException(ChatMessageErrorStatus.INVALID_MESSAGE_TYPE);

        // DTO 타입을 엔티티 enum 타입으로 변환
        return ChatMessage.MessageType.valueOf(type.name());
    }

    public static String summarize(String content, int maxLength) {
        if (content == null) return "";
        String trimmed = content.trim();
        if (trimmed.isEmpty()) return "";

        // 개행 제거(알림 문구 깨짐 방지)
        String singleLine = trimmed.replaceAll("\\s+", " ");

        if (singleLine.length() <= maxLength) {
            return singleLine;
        }
        return singleLine.substring(0, maxLength) + "...";
    }
}