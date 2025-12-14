package com.shu.backend.domain.school.exception.status;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SchoolErrorStatus implements BaseErrorCode {
    //400
    SCHOOL_NOT_FOUND(HttpStatus.NOT_FOUND, "SCHOOL4000", "존재하지 않는 학교입니다."),
    REQUEST_IMAGE_REQUIRED(HttpStatus.NOT_FOUND, "SCHOOL4001", "이미지는 필수입니다."),
    INVALID_SCHOOL_FOR_SIGNUP(HttpStatus.BAD_REQUEST, "SCHOOL4002", "해당 학교로는 회원가입할 수 없습니다.");


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
