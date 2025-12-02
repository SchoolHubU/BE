package com.shu.backend.domain.board.exception;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class BoardException extends GeneralException {

    public BoardException(BaseErrorCode code) {
        super(code);
    }
}
