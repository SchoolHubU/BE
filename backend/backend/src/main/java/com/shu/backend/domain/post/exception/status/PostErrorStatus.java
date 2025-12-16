package com.shu.backend.domain.post.exception.status;



import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PostErrorStatus implements BaseErrorCode {
    //400
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST4000", "존재하지 않는 게시글입니다."),
    INVALID_REGION_NAME(HttpStatus.BAD_REQUEST, "REGION4002", "잘못된 지역명입니다."),
    REGION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "REGION4003", "이미 존재하는 지역명입니다.");



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
