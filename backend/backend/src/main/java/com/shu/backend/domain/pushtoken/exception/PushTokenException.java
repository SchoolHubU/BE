package com.shu.backend.domain.pushtoken.exception;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class PushTokenException extends GeneralException {
    public PushTokenException(BaseErrorCode code) {
        super(code);
    }
}
