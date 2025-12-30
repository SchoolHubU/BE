package com.shu.backend.domain.notification.entity;

import com.shu.backend.domain.notification.enums.NotificationTargetType;
import com.shu.backend.domain.notification.enums.NotificationType;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // 무슨 알림인지 (대댓글, 내 글 좋아요 등)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    // 알림이 어떤 엔티티에 대한 것인지 (POST, COMMENT 등)
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private NotificationTargetType targetType;

    // 실제 엔티티의 pk
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    // 알림 문구
    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    // 알림을 받는 유저
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 알림을 일으킨 유저 (SYSTEM 알림일 경우 null)
    @Column(name = "actor_id")
    private Long actorId;

    // ===== 생성 메서드 ===== //
    public static Notification create(
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            String message,
            Long userId,
            Long actorId
    ) {
        Notification n = new Notification();
        n.type = type;
        n.targetType = targetType;
        n.targetId = targetId;
        n.message = message;
        n.userId = userId;
        n.actorId = actorId;
        n.isRead = false;
        return n;
    }

    public void markAsRead() {
        this.isRead = true;
    }

}