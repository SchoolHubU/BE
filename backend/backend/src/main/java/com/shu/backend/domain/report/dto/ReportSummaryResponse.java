package com.shu.backend.domain.report.dto;

import com.shu.backend.domain.report.entity.Report;
import com.shu.backend.domain.report.enums.ReportStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 신고목록 조회용 DTO
 */
@Getter
@Builder
public class ReportSummaryResponse {

    private Long reportId;
    private Long reporterId;
    private Long reportedUserId;
    private String targetType;
    private Long targetId;
    private String reportReason;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public static ReportSummaryResponse from(Report r) {
        return ReportSummaryResponse.builder()
                .reportId(r.getId())
                .reporterId(r.getReporter().getId())
                .reportedUserId(r.getReportedUser().getId())
                .targetType(r.getTargetType().name())
                .targetId(r.getTargetId())
                .reportReason(r.getReportReason().name())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .processedAt(r.getProcessedAt())
                .build();
    }

}
