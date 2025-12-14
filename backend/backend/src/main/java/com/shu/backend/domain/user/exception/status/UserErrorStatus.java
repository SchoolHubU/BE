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
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER4000", "존재하지 않는 사용자입니다."),
    EXIST_EMAIL(HttpStatus.NOT_FOUND, "USER4001", "이미 존재하는 이메일입니다."),
    EXIST_NICKNAME(HttpStatus.NOT_FOUND, "USER4002", "이미 존재하는 닉네임입니다."),
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "USER4003", "이미 존재하는 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "USER4004", "비밀번호가 일치하지 않습니다."),
    SCHOOL_VERIFICATION_ALREADY_APPROVED(HttpStatus.BAD_REQUEST,"USER4005" ,"이미 학교 인증이 완료된 사용자입니다."),

    //403
    SCHOOL_VERIFICATION_REQUIRED(HttpStatus.FORBIDDEN, "USER4031", "학교 인증이 필요합니다. 학생증을 업로드해주세요."),
    SCHOOL_VERIFICATION_PENDING(HttpStatus.FORBIDDEN, "USER4032", "학교 인증 심사 중입니다. 인증 완료 후 로그인 가능합니다."),
    SCHOOL_VERIFICATION_REJECTED(HttpStatus.FORBIDDEN, "USER4033", "학교 인증이 거절되었습니다."),

    //500
    USER_STUDENT_CARD_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "USER5000", "학생증 이미지 업로드에 실패했습니다."),
    SCHOOL_VERIFICATION_STATUS_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "USER5001", "잘못된 인증 상태입니다. 관리자에게 문의해주세요.");

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
