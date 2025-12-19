package com.shu.backend.domain.chatroom.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class ChatRoomDTO {

    @Getter @Setter
    public static class CreateDmRequest {
        private Long otherUserId;
    }

    @Getter @Builder
    public static class CreateDmResponse {
        private Long roomId;
        private Long otherUserId;
        private String displayName;
        private LocalDateTime lastMessageAt;

    }

    @Getter @Builder
    public static class RoomListItem {
        private Long roomId;
        private Long otherUserId;

        private String lastPreview;      // 텍스트면 content, 이미지면 "사진"
        private LocalDateTime lastMessageAt;

        private String displayName;

        private long unreadCount;
    }

    @Getter @Builder
    public static class RoomListResponse {
        private List<RoomListItem> rooms;
    }
}