package com.shu.backend.domain.comment.exception.status;



import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommentErrorStatus implements BaseErrorCode {

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT4000", "존재하지 않는 댓글입니다."),
    NO_PERMISSION_TO_WRITE(HttpStatus.BAD_REQUEST, "COMMENT4001", "댓글 작성 권한이 없습니다."),
    COMMENT_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "COMMENT4002", "이미 삭제된 댓글입니다."),
    PARENT_COMMENT_NOT_IN_SAME_POST(HttpStatus.BAD_REQUEST, "COMMENT4003", "댓글과 대댓글의 게시글이 다릅니다."),
    INVALID_CONTENT(HttpStatus.BAD_REQUEST, "COMMENT4004", "댓글 내용이 잘못되었습니다."),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "COMMENT4005", "댓글 수정 권한이 없습니다.");



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
