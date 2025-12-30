package com.shu.backend.domain.notification.exception.status;



import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorStatus implements BaseErrorCode {

    INVALID_NOTIFICATION_REQUEST(HttpStatus.BAD_REQUEST, "NOTIFICATION_4001", "알림 요청이 올바르지 않습니다."),
    MESSAGE_REQUIRED(HttpStatus.BAD_REQUEST, "NOTIFICATION_4002", "알림 메시지는 필수입니다."),
    RECEIVER_REQUIRED(HttpStatus.BAD_REQUEST, "NOTIFICATION_4003", "알림 수신자는 필수입니다."),

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_4041", "알림을 찾을 수 없습니다."),
    NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "NOTIFICATION_4031", "해당 알림에 접근할 권한이 없습니다.");




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
