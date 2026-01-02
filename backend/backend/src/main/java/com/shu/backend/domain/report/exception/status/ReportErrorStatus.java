package com.shu.backend.domain.report.exception.status;



import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReportErrorStatus implements BaseErrorCode {

    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT4001", "신고 내역을 찾을 수 없습니다."),
    DUPLICATE_REPORT(HttpStatus.CONFLICT, "REPORT4002", "이미 동일 대상에 대한 신고가 존재합니다."),
    SELF_REPORT_FORBIDDEN(HttpStatus.BAD_REQUEST, "REPORT4003", "자기 자신을 신고할 수 없습니다."),
    REPORT_NOT_PENDING(HttpStatus.BAD_REQUEST, "REPORT4004", "이미 처리된 신고는 다시 처리할 수 없습니다."),
    UNSUPPORTED_TARGET_TYPE(HttpStatus.BAD_REQUEST, "REPORT4005", "지원하지 않는 신고 대상 타입입니다."),
    PENALTY_ALREADY_CREATED(HttpStatus.CONFLICT, "REPORT4006", "해당 신고로 이미 제재가 생성되었습니다.");



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
