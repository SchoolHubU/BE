package com.shu.backend.domain.comment.exception;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class CommentException extends GeneralException {
    public CommentException(BaseErrorCode code) {
        super(code);
    }
}
