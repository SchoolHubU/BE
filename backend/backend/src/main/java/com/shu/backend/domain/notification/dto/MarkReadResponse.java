package com.shu.backend.domain.notification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MarkReadResponse {
    private Long notificationId;
    private Boolean isRead;

    public static MarkReadResponse of(Long id) {
        return MarkReadResponse.builder()
                .notificationId(id)
                .isRead(true)
                .build();
    }
}