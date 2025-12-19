package com.shu.backend.domain.media.exception;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class MediaException extends GeneralException {
    public MediaException(BaseErrorCode code) {
        super(code);
    }
}
