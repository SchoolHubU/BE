package com.shu.backend.domain.user.exception.status;

import com.shu.backend.global.apiPayload.code.BaseCode;
import com.shu.backend.global.apiPayload.code.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserSuccessStatus implements BaseCode {
    // Auth
    USER_SIGNUP_SUCCESS(HttpStatus.CREATED, "USER2010", "회원가입 완료"),
    USER_LOGIN_SUCCESS(HttpStatus.OK, "USER2001", "로그인 성공"),

    // User
    USER_FOUND(HttpStatus.OK, "USER2002", "사용자 조회 성공"),

    // Verification
    VERIFICATION_APPROVED(HttpStatus.OK, "VERI2001", "학교 인증 승인 완료");

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
