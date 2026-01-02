package com.shu.backend.domain.penalty.entity;

import com.shu.backend.domain.penalty.enums.PenaltyStatus;
import com.shu.backend.domain.report.entity.Report;
import com.shu.backend.domain.report.enums.ReportReason;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "penalty",
        indexes = {
                @Index(name = "idx_penalty_user_expires", columnList = "user_id,expires_at")
        }
)
public class Penalty extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private PenaltyStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;
}