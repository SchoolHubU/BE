package com.shu.backend.domain.chatmessage.exception.status;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import com.shu.backend.global.apiPayload.code.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatMessageErrorStatus implements BaseErrorCode {

    //예시
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHATMSG4001", "채팅방을 찾을 수 없습니다."),
    NOT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "CHATMSG4002", "채팅방 참여자가 아닙니다."),
    CHAT_BLOCKED(HttpStatus.FORBIDDEN, "CHATMSG4003", "차단 상태에서는 채팅할 수 없습니다."),
    INVALID_MESSAGE_TYPE(HttpStatus.BAD_REQUEST, "CHATMSG4004", "유효하지 않은 메시지 타입입니다."),
    IMAGE_URL_REQUIRED(HttpStatus.BAD_REQUEST, "CHATMSG4005", "이미지 메시지에는 imageUrl이 필요합니다."),

    // WebSocket 인증/인가
    UNAUTHORIZED_WS(HttpStatus.UNAUTHORIZED, "CHATMSG4010", "WebSocket 인증이 필요합니다."),
    INVALID_WS_TOKEN(HttpStatus.UNAUTHORIZED, "CHATMSG4011", "유효하지 않은 WebSocket 토큰입니다."),
    WS_NOT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "CHATMSG4012", "채팅방 구독 권한이 없습니다."),

    CHAT_IMAGE_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "CHATMSG5001", "채팅 이미지 업로드에 실패했습니다.");


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
