package com.shu.backend.domain.report.controller;

import com.shu.backend.domain.report.dto.ReportDTO;
import com.shu.backend.domain.report.exception.status.ReportSuccessStatus;
import com.shu.backend.domain.report.service.ReportService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Report",
        description = "신고 관련 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    @Operation(
            summary = "신고 생성",
            description = """
                    글/댓글/채팅(메시지)에 대한 신고를 생성합니다.
                    - 신고는 PENDING 상태로 생성됩니다.
                    - 동일 사용자/동일 대상(targetType+targetId)에 대한 중복 신고는 차단됩니다.
                    - 제재는 관리자 승인 시에만 생성됩니다.
                    """
    )
    @PostMapping
    public ApiResponse<ReportDTO.CreateResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ReportDTO.CreateRequest req
    ){
        Long reporterId = reportService.create(user.getId(), req);

        return ApiResponse.of(ReportSuccessStatus.REPORT_CREATE_SUCCESS,
                ReportDTO.CreateResponse.builder().reportId(reporterId).build());

    }
}
