package com.shu.backend.domain.chatmessage.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class ChatMessageDTO {

    public enum MessageType { TEXT, IMAGE }

    // =================== 전송 ===================
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SendRequest {
        private Long roomId;
        private MessageType type;
        private String content;   // TEXT
        private String imageUrl;  // IMAGE
    }

    // =================== 업로드 응답 ===================
    @Getter
    @AllArgsConstructor
    public static class UploadImageResponse {
        private String imageUrl;
    }

    // =================== 메시지 응답 ===================
    @Getter
    @Builder
    public static class MessageResponse {
        private Long messageId;
        private Long roomId;
        private Long senderId;

        private MessageType type;
        private String content;
        private List<MediaItem> medias;

        private LocalDateTime createdAt;
    }

    // =================== 미디어 ===================
    @Getter
    @Builder
    public static class MediaItem {
        private Long id;
        private String url;
        private String mediaType;
    }

    // =================== 메시지 목록 ===================
    @Getter
    @Builder
    public static class MessageListResponse {
        private Long roomId;
        private List<MessageResponse> messages;
    }

    // =================== 읽음 처리 ===================
    @Getter
    @Setter
    public static class ReadRequest {
        private Long messageId;
    }
}
