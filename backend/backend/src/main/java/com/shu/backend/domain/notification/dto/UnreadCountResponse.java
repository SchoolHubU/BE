package com.shu.backend.domain.notification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnreadCountResponse {
    private long unreadCount;

    public static UnreadCountResponse of(long count) {
        return UnreadCountResponse.builder().unreadCount(count).build();
    }
}