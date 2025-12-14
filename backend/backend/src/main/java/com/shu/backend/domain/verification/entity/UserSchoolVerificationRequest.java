package com.shu.backend.domain.verification.entity;

import com.shu.backend.domain.school.School;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.verification.status.VerificationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class UserSchoolVerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private java.lang.Long id;

    @NotNull
    @Column(name = "request_image_url", nullable = false)
    private String requestImageUrl;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "status", nullable = false)
    private VerificationStatus status;

    @Column(name = "admin_comment")
    private String adminComment;

    @NotNull
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by")
    private java.lang.Long processedBy;   // 운영자 userId

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Builder
    public UserSchoolVerificationRequest(String requestImageUrl, User user, School school) {
        this.requestImageUrl = requestImageUrl;
        this.user = user;
        this.school = school;

        // 기본값 설정
        this.status = VerificationStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    // 승인 처리
    public void approve(Long adminUserId, String adminComment) {
        this.status = VerificationStatus.APPROVED;
        this.processedBy = adminUserId;
        this.processedAt = LocalDateTime.now();
        this.adminComment = adminComment;
    }

    // 거절 처리
    public void reject(Long adminUserId, String adminComment) {
        this.status = VerificationStatus.REJECTED;
        this.processedBy = adminUserId;
        this.processedAt = LocalDateTime.now();
        this.adminComment = adminComment;
    }
}
