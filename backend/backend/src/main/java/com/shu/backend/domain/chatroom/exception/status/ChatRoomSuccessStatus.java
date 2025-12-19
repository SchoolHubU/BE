package com.shu.backend.domain.chatroom.exception.status;

import com.shu.backend.global.apiPayload.code.BaseCode;
import com.shu.backend.global.apiPayload.code.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
@AllArgsConstructor
public enum ChatRoomSuccessStatus implements BaseCode {

    DM_ROOM_FIND_OR_CREATE_SUCCESS(HttpStatus.OK,"CHATROOM2001","DM 채팅방 생성 또는 조회 성공"),
    CHAT_ROOM_LIST_SUCCESS(HttpStatus.OK,"CHATROOM2002","채팅방 목록 조회 성공"),
    CHAT_ROOM_LEAVE_SUCCESS(HttpStatus.OK,"CHATROOM2003","채팅방 나가기 성공"),
    CHAT_ROOM_BLOCK_SUCCESS(HttpStatus.OK,"CHATROOM2004","채팅방 차단 성공");

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
