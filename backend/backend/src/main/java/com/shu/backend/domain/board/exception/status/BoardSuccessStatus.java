package com.shu.backend.domain.board.exception.status;

import com.shu.backend.global.apiPayload.code.BaseCode;
import com.shu.backend.global.apiPayload.code.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BoardSuccessStatus implements BaseCode {

    _BOARD_CREATED(HttpStatus.OK, "BOARD2000", "게시판이 성공적으로 생성되었습니다.")


    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;


    @Override
    public ReasonDto getReasonHttpStatus() {
        return null;
    }

    @Override
    public ReasonDto getReason() {
        return null;
    }
}
