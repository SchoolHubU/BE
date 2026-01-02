package com.shu.backend.domain.penalty.dto;

import com.shu.backend.domain.penalty.entity.Penalty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class PenaltyDTO {

    @Getter
    @Builder
    public static class MyActiveResponse {
        private boolean penalized;
        private LocalDateTime expiresAt;   // penalized=false면 null
        private String reason;            // penalized=false면 null
        private Long reportId;
    }

    @Getter
    @Builder
    public static class SummaryResponse {
        private Long penaltyId;
        private Long userId;
        private Long reportId;
        private String reason;
        private LocalDateTime expiresAt;
        private LocalDateTime createdAt;

        public static SummaryResponse from(Penalty p) {
            return SummaryResponse.builder()
                    .penaltyId(p.getId())
                    .userId(p.getUser().getId())
                    .reportId(p.getReport().getId())
                    .reason(p.getReason().name())
                    .expiresAt(p.getExpiresAt())
                    .createdAt(p.getCreatedAt())
                    .build();
        }
    }
}

