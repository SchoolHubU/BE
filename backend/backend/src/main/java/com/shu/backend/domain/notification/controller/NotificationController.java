package com.shu.backend.domain.notification.controller;

import com.shu.backend.domain.notification.dto.NotificationResponse;
import com.shu.backend.domain.notification.dto.UnreadCountResponse;
import com.shu.backend.domain.notification.exception.status.NotificationSuccessStatus;
import com.shu.backend.domain.notification.service.NotificationService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Notification",
        description = "알림 관련 API"
)
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "내 알림 조회",
            description = "내 알림을 모두 조회합니다."
    )
    @GetMapping
    public ApiResponse<Slice<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Slice<NotificationResponse> myNotifications = notificationService.getMyNotification(user.getId(), page, size);

        return ApiResponse.of(NotificationSuccessStatus.NOTIFICATION_CREATE_SUCCESS, myNotifications);
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(@AuthenticationPrincipal User user) {
        UnreadCountResponse unreadCountResponse = notificationService.getUnreadCount(user.getId());

        return ApiResponse.of(NotificationSuccessStatus.NOTIFICATION_GET_SUCCESS, unreadCountResponse);
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Long> markAsRead(@AuthenticationPrincipal User user,
                                        @PathVariable Long id) {
        notificationService.markAsRead(user.getId(), id);

        return ApiResponse.of(NotificationSuccessStatus.NOTIFICATION_GET_SUCCESS, id);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Integer> markAllAsRead(@AuthenticationPrincipal User user) {
        int count = notificationService.markAllAsRead(user.getId());

        return ApiResponse.of(NotificationSuccessStatus.NOTIFICATION_GET_SUCCESS, count);
    }

}
