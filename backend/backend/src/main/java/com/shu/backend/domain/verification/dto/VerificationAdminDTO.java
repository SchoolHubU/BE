package com.shu.backend.domain.verification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shu.backend.domain.verification.status.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class VerificationAdminDTO {

    // ===================== Request =====================

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "VerificationDecisionRequest", description = "인증 승인/거절 요청 DTO")
    public static class DecisionRequest {

        @NotBlank(message = "관리자 코멘트는 필수입니다.")
        @Size(min = 2, max = 200, message = "관리자 코멘트는 2~200자여야 합니다.")
        @Schema(description = "관리자 코멘트", example = "학생증 확인 완료", requiredMode = Schema.RequiredMode.REQUIRED)
        private String adminComment;
    }

    // ===================== Response =====================
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "VerificationRequestListItem", description = "인증 요청 목록 아이템")
    public static class ListItemResponse {

        @Schema(description = "인증 요청 ID", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long requestId;

        @Schema(description = "요청 상태", example = "PENDING", requiredMode = Schema.RequiredMode.REQUIRED)
        private VerificationStatus status;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "요청 시각", example = "2025-12-14 15:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime requestedAt;

        @Schema(description = "유저 ID", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long userId;

        @Schema(description = "유저 이메일", example = "test@naver.com", requiredMode = Schema.RequiredMode.REQUIRED)
        private String userEmail;

        @Schema(description = "학교 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long schoolId;

        @Schema(description = "학교명", example = "서울고등학교", requiredMode = Schema.RequiredMode.REQUIRED)
        private String schoolName;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "VerificationRequestDetail", description = "인증 요청 상세")
    public static class DetailResponse {

        @Schema(description = "인증 요청 ID", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long requestId;

        @Schema(description = "학생증/재학증명서 이미지 URL", example = "https://.../studentcard.png", requiredMode = Schema.RequiredMode.REQUIRED)
        private String requestImageUrl;

        @Schema(description = "요청 상태", example = "PENDING", requiredMode = Schema.RequiredMode.REQUIRED)
        private VerificationStatus status;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "요청 시각", example = "2025-12-14 15:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime requestedAt;

        @Schema(description = "유저 ID", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long userId;

        @Schema(description = "유저 이메일", example = "test@naver.com", requiredMode = Schema.RequiredMode.REQUIRED)
        private String userEmail;

        @Schema(description = "학교 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long schoolId;

        @Schema(description = "학교명", example = "서울고등학교", requiredMode = Schema.RequiredMode.REQUIRED)
        private String schoolName;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "처리 시각(PENDING이면 null)", example = "2025-12-14 16:10:00", nullable = true)
        private LocalDateTime processedAt;

        @Schema(description = "처리 관리자 ID(PENDING이면 null)", example = "100", nullable = true)
        private Long processedBy;

        @Schema(description = "관리자 코멘트(PENDING이면 null)", example = "이미지 확인 완료", nullable = true)
        private String adminComment;
    }

    // ===================== Optional: 목록 응답 Wrapper =====================
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "VerificationRequestListResponse", description = "인증 요청 목록 응답")
    public static class ListResponse {

        @Schema(description = "목록", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<ListItemResponse> items;
    }
}