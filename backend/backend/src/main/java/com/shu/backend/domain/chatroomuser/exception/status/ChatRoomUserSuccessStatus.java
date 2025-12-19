package com.shu.backend.domain.chatroomuser.exception.status;

import com.shu.backend.global.apiPayload.code.BaseCode;
import com.shu.backend.global.apiPayload.code.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatRoomUserSuccessStatus implements BaseCode {

    //예시
    BOARD_FOUND_SUCCESS(HttpStatus.OK, "BOARD2001", "게시판 조회 성공");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;


    @Override
    public ReasonDto getReasonHttpStatus() {
        return ReasonDto.builder()
                .httpStatus(httpStatus)
                .isSuccess(true)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ReasonDto getReason() {
        return ReasonDto.builder()
                .isSuccess(true)
                .code(code)
                .message(message)
                .build();
    }
}
