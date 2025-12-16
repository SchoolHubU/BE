package com.shu.backend.domain.board.exception.status;

import com.shu.backend.global.apiPayload.code.BaseCode;
import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import com.shu.backend.global.apiPayload.code.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BoardErrorStatus implements BaseErrorCode {

    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "BOARD4000", "게시판이 존재하지 않습니다."),
    BOARD_INACTIVE(HttpStatus.FORBIDDEN, "BOARD4001", "비활성화된 게시판입니다."),
    INVALID_BOARD_TITLE(HttpStatus.BAD_REQUEST, "BOARD4002", "잘못된 게시판 제목입니다."),
    SCHOOL_ID_REQUIRED(HttpStatus.BAD_REQUEST, "BOARD4003", "학교 ID가 필요합니다."),
    REGION_ID_REQUIRED(HttpStatus.BAD_REQUEST, "BOARD4004", "지역 ID가 필요합니다."),
    INVALID_BOARD_SCOPE(HttpStatus.INTERNAL_SERVER_ERROR, "BOARD5000", "잘못된 게시판 타입입니다.");

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
