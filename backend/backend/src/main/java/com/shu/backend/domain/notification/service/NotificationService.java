package com.shu.backend.domain.notification.service;

import com.shu.backend.domain.notification.dto.NotificationResponse;
import com.shu.backend.domain.notification.dto.UnreadCountResponse;
import com.shu.backend.domain.notification.entity.Notification;
import com.shu.backend.domain.notification.enums.NotificationTargetType;
import com.shu.backend.domain.notification.enums.NotificationType;
import com.shu.backend.domain.notification.exception.NotificationException;
import com.shu.backend.domain.notification.exception.status.NotificationErrorStatus;
import com.shu.backend.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;

    @Transactional
    public Long create(NotificationType type,
                       NotificationTargetType targetType,
                       Long targetId,
                       String message,
                       Long receiverUserId,
                       Long actorId) {
        if (receiverUserId == null){
            throw new NotificationException(NotificationErrorStatus.RECEIVER_REQUIRED);
        }

        if (message == null){
            throw new NotificationException(NotificationErrorStatus.MESSAGE_REQUIRED);
        }

        if (actorId != null && actorId == receiverUserId){
            return actorId;
        }

        Notification n = Notification.create(type, targetType, targetId, message, receiverUserId, actorId);
        Notification saved = notificationRepository.save(n);
        return saved.getId();
    }

    public Slice<NotificationResponse> getMyNotification(Long userId, int page, int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    public UnreadCountResponse getUnreadCount(Long userId) {
        return UnreadCountResponse.of(notificationRepository.countByUserIdAndIsReadFalse(userId));
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationException(NotificationErrorStatus.NOTIFICATION_NOT_FOUND));

        if (!Boolean.TRUE.equals(n.getIsRead())) {
            n.markAsRead();
        }
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId);
    }



}
