package com.shu.backend.domain.report;

import com.shu.backend.domain.report.enums.ReportReason;
import com.shu.backend.domain.report.enums.TargetType;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    @Column(nullable = false)
    private Boolean isProcessed = false;

    // 처리 날짜
    private LocalDateTime processedAt;

    // 처리 함수
    public void markProcessed() {
        this.isProcessed = true;
        this.processedAt = LocalDateTime.now();
    }


    //추후 Snapshot 도메인 및 Snapshot 필드 추가 예정


}