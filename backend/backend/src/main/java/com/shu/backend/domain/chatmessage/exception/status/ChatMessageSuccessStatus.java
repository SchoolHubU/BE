package com.shu.backend.domain.chatmessage.exception.status;

import com.shu.backend.global.apiPayload.code.BaseCode;
import com.shu.backend.global.apiPayload.code.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatMessageSuccessStatus implements BaseCode {


    // =================== 메시지 ===================
    CHAT_MESSAGE_LIST_SUCCESS(HttpStatus.OK,"CHATMSG2001","채팅 메시지 목록 조회 성공"),
    CHAT_MESSAGE_READ_SUCCESS(HttpStatus.OK,"CHATMSG2002","채팅 읽음 처리 성공"),
    CHAT_MESSAGE_SEND_SUCCESS(HttpStatus.OK,"CHATMSG2003","채팅 메시지 전송 성공"),

    // =================== 이미지 ===================
    CHAT_IMAGE_UPLOAD_SUCCESS(HttpStatus.OK, "CHATMSG2004", "채팅 이미지 업로드 성공");

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
