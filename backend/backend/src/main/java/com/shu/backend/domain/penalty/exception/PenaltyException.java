package com.shu.backend.domain.penalty.exception;


import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class PenaltyException extends GeneralException {
  public PenaltyException(BaseErrorCode code) {
    super(code);
  }
}
