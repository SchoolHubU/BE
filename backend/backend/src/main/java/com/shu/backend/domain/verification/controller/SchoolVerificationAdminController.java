package com.shu.backend.domain.verification.controller;

import com.shu.backend.domain.verification.dto.VerificationAdminDTO;
import com.shu.backend.domain.verification.exception.VerificationSuccessStatus;
import com.shu.backend.domain.verification.service.SchoolVerificationAdminService;
import com.shu.backend.domain.verification.status.VerificationStatus;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Tag(name = "Admin Verification", description = "운영진 학교 인증 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/verification-requests")
public class SchoolVerificationAdminController {

    private final SchoolVerificationAdminService adminService;

    @Operation(
            summary = "학교 인증 요청 목록 조회",
            description = "운영진이 학교 인증 요청을 상태별로 조회합니다. 기본값은 PENDING 입니다."
    )
    @GetMapping
    public ApiResponse<List<VerificationAdminDTO.ListItemResponse>> list(
            @Parameter(description = "조회할 상태", example = "PENDING")
            @RequestParam(defaultValue = "PENDING") VerificationStatus status
    ) {
        return ApiResponse.of(
                VerificationSuccessStatus.VERIFICATION_REQUEST_LIST_SUCCESS,
                adminService.list(status)
        );
    }

    @Operation(
            summary = "학교 인증 요청 상세 조회",
            description = "운영진이 특정 인증 요청의 상세(이미지 URL, 처리 정보 등)를 조회합니다."
    )
    @GetMapping("/{requestId}")
    public ApiResponse<VerificationAdminDTO.DetailResponse> detail(
            @Parameter(description = "인증 요청 ID", example = "12", required = true)
            @PathVariable Long requestId
    ) {
        return ApiResponse.of(
                VerificationSuccessStatus.VERIFICATION_REQUEST_DETAIL_SUCCESS,
                adminService.detail(requestId)
        );
    }

    @Operation(
            summary = "학교 인증 요청 승인",
            description = "운영진이 인증 요청을 승인합니다. 승인 시 최종 인증 정보를 생성하고 유저를 인증 완료로 변경합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "승인 처리 요청 바디",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VerificationAdminDTO.DecisionRequest.class)
                    )
            )
    )
    @PatchMapping("/{requestId}/approve")
    public ApiResponse<Void> approve(
            @Parameter(description = "인증 요청 ID", example = "12", required = true)
            @PathVariable Long requestId,
            @RequestBody @Valid VerificationAdminDTO.DecisionRequest request
    ) {
        adminService.approve(requestId, request.getAdminComment());
        return ApiResponse.of(VerificationSuccessStatus.VERIFICATION_APPROVE_SUCCESS, null);
    }

    @Operation(
            summary = "학교 인증 요청 거절",
            description = "운영진이 인증 요청을 거절합니다. 거절 시 요청 상태만 REJECTED로 변경됩니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "거절 처리 요청 바디",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VerificationAdminDTO.DecisionRequest.class)
                    )
            )
    )
    @PatchMapping("/{requestId}/reject")
    public ApiResponse<Void> reject(
            @Parameter(description = "인증 요청 ID", example = "12", required = true)
            @PathVariable Long requestId,
            @RequestBody @Valid VerificationAdminDTO.DecisionRequest request
    ) {
        adminService.reject(requestId, request.getAdminComment());
        return ApiResponse.of(VerificationSuccessStatus.VERIFICATION_REJECT_SUCCESS, null);
    }
}