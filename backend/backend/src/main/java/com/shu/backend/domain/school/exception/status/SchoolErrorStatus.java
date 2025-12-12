package com.shu.backend.domain.school.exception.status;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SchoolErrorStatus implements BaseErrorCode {
    SCHOOL_NOT_FOUND(HttpStatus.NOT_FOUND, "SCHOOL4040", "존재하지 않는 학교입니다."),
    REQUEST_IMAGE_REQUIRED(HttpStatus.NOT_FOUND, "SCHOOL4041", "이미지는 필수입니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDto getReason() {
        return ErrorReasonDto.builder()
                .isSuccess(false)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ErrorReasonDto getReasonHttpStatus() {
        return ErrorReasonDto.builder()
                .httpStatus(httpStatus)
                .isSuccess(false)
                .code(code)
                .message(message)
                .build();
    }
}
