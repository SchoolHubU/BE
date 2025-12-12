package com.shu.backend.domain.school.exception;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class SchoolException extends GeneralException {
    public SchoolException(BaseErrorCode code) {
        super(code);
    }
}
