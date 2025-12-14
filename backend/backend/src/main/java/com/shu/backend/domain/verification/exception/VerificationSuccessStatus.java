package com.shu.backend.domain.verification.exception;

import com.shu.backend.global.apiPayload.code.BaseCode;
import com.shu.backend.global.apiPayload.code.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum VerificationSuccessStatus implements BaseCode {

    // Verification 예시
    VERIFICATION_REQUEST_LIST_SUCCESS(HttpStatus.OK, "VERIFICATION2000", "학교 인증 요청 목록 조회 성공"),
    VERIFICATION_REQUEST_DETAIL_SUCCESS(HttpStatus.OK, "VERIFICATION2001", "학교 인증 요청 상세 조회 성공"),
    VERIFICATION_APPROVE_SUCCESS(HttpStatus.OK, "VERIFICATION2002", "학교 인증 요청 승인 성공"),
    VERIFICATION_REJECT_SUCCESS(HttpStatus.OK, "VERIFICATION2003", "학교 인증 요청 거절 성공");

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

