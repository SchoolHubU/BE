package com.shu.backend.domain.verification.exception;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum VerificationErrorStatus implements BaseErrorCode {
    VERIFICATION_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "VERIFICATION4000", "학교 인증 요청을 찾을 수 없습니다."),
    VERIFICATION_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "VERIFICATION4001", "이미 처리된 인증 요청입니다."),
    VERIFICATION_STATUS_INVALID(HttpStatus.BAD_REQUEST, "VERIFICATION4002", "유효하지 않은 인증 상태입니다."),
    SCHOOL_VERIFICATION_STATUS_INVALID(HttpStatus.BAD_REQUEST, "VERIFICATION4003", "이미 학교 인증이 완료된 사용자입니다."),
    VERIFICATION_ADMIN_ONLY(HttpStatus.FORBIDDEN, "VERIFICATION4030", "관리자만 처리할 수 있습니다."),
    USER_STUDENT_CARD_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "VERIFICATION5000", "학생증 이미지 업로드에 실패했습니다."),
    VERIFICATION_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "VERIFICATION5001", "학교 인증 처리 중 서버 오류가 발생했습니다.");

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


