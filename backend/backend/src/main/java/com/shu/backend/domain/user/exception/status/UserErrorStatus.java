package com.shu.backend.domain.user.exception.status;

import com.shu.backend.global.apiPayload.code.BaseCode;
import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorStatus implements BaseErrorCode {

    //400
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER4040", "존재하지 않는 사용자입니다."),
    EXIST_EMAIL(HttpStatus.NOT_FOUND, "USER4041", "이미 존재하는 이메일입니다."),
    EXIST_NICKNAME(HttpStatus.NOT_FOUND, "USER4042", "이미 존재하는 닉네임입니다."),

    //500
    USER_STUDENT_CARD_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "USER5000", "학생증 이미지 업로드에 실패했습니다.");


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
