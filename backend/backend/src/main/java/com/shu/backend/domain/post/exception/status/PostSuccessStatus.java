package com.shu.backend.domain.post.exception.status;


import com.shu.backend.global.apiPayload.code.BaseCode;
import com.shu.backend.global.apiPayload.code.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PostSuccessStatus implements BaseCode {

    POST_CREATE_SUCCESS(HttpStatus.OK, "POST2000", "게시글 작성이 완료되었습니다."),
    POST_UPDATE_SUCCESS(HttpStatus.OK, "POST2001", "게시글 수정이 완료되었습니다."),
    POST_DELETE_SUCCESS(HttpStatus.OK, "POST2002", "게시글 삭제가 완료되었습니다.");

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
