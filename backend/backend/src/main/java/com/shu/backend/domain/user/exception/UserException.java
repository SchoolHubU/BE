package com.shu.backend.domain.user.exception;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class UserException extends GeneralException {

    public UserException(BaseErrorCode code) {
        super(code);
    }
}
