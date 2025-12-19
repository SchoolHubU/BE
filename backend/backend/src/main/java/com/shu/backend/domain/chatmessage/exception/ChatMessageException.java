package com.shu.backend.domain.chatmessage.exception;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class ChatMessageException extends GeneralException {

    public ChatMessageException(BaseErrorCode code) {
        super(code);
    }
}
