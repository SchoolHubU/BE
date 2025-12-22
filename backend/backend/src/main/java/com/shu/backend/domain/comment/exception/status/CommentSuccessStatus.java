package com.shu.backend.domain.comment.exception.status;


import com.shu.backend.global.apiPayload.code.BaseCode;
import com.shu.backend.global.apiPayload.code.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommentSuccessStatus implements BaseCode {

    COMMENT_CREATE_SUCCESS(HttpStatus.OK, "COMMENT2000", "댓글 작성이 완료되었습니다."),
    COMMENT_UPDATE_SUCCESS(HttpStatus.OK, "COMMENT2001", "댓글 수정이 완료되었습니다."),
    COMMENT_DELETE_SUCCESS(HttpStatus.OK, "COMMENT2002", "댓글 삭제가 완료되었습니다.");

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
