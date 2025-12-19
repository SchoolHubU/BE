package com.shu.backend.domain.chatroom.exception;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class ChatRoomException extends GeneralException {

    public ChatRoomException(BaseErrorCode code) {
        super(code);
    }
}
