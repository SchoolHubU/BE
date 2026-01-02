package com.shu.backend.domain.report.exception.status;


import com.shu.backend.global.apiPayload.code.BaseCode;
import com.shu.backend.global.apiPayload.code.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReportSuccessStatus implements BaseCode {

    REPORT_CREATE_SUCCESS(HttpStatus.OK, "REPORT2000", "신고 생성이 완료되었습니다."),
    REPORT_LIST_SUCCESS(HttpStatus.OK, "REPORT2001", "신고 목록 조회가 완료되었습니다."),
    REPORT_APPROVE_SUCCESS(HttpStatus.OK, "REPORT2002", "신고 승인이 완료되었습니다."),
    REPORT_REJECT_SUCCESS(HttpStatus.OK, "REPORT2003", "신고 반려가 완료되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDto getReason() {
        return ReasonDto.builder()
                .isSuccess(true)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ReasonDto getReasonHttpStatus() {
        return ReasonDto.builder()
                .httpStatus(httpStatus)
                .isSuccess(true)
                .code(code)
                .message(message)
                .build();
    }
}
