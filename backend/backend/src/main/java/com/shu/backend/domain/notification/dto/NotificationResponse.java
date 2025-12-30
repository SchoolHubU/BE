package com.shu.backend.domain.notification.dto;

import com.shu.backend.domain.notification.entity.Notification;
import com.shu.backend.domain.notification.enums.NotificationTargetType;
import com.shu.backend.domain.notification.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private NotificationTargetType targetType;
    private Long targetId;

    private String message;
    private Boolean isRead;

    private Long actorId;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .targetType(n.getTargetType())
                .targetId(n.getTargetId())
                .message(n.getMessage())
                .isRead(n.getIsRead())
                .actorId(n.getActorId())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
