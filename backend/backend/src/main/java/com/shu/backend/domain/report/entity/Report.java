package com.shu.backend.domain.report.entity;

import com.shu.backend.domain.report.enums.ReportReason;
import com.shu.backend.domain.report.enums.ReportStatus;
import com.shu.backend.domain.report.enums.TargetType;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Table(
        name = "report",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_report_reporter_target",
                columnNames = {"reporter_id", "target_type", "target_id"}
        ),
        indexes = {
                @Index(name = "idx_report_target", columnList = "target_type,target_id"),
                @Index(name = "idx_report_processed_created", columnList = "is_processed,created_at")
        }
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private User reportedUser;

    // 신고 대상 종류 (POST/COMMENT/CHAT)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    // 신고 대상 ID (postId, CommentId 등)
    @Column(nullable = false)
    private Long targetId;

    // 신고 이유 (욕설, 선정적 내용, 광고 등)
    @Enumerated(EnumType.STRING)
    @Column(name = "report_reason", nullable = false)
    private ReportReason reportReason;

    // 처리 진행 상황
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    // 처리자(admin)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private User handledBy;

    // 처리 날짜
    private LocalDateTime processedAt;

    public void resolve(User handler) {
        this.status = ReportStatus.RESOLVED;
        this.processedAt = LocalDateTime.now();
        this.handledBy = handler;
    }

    public void reject(User handler) {
        this.status = ReportStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
        this.handledBy = handler;
    }

    public boolean isPending() {
        return this.status == ReportStatus.PENDING;
    }


    //추후 Snapshot 도메인 및 Snapshot 필드 추가 예정


}