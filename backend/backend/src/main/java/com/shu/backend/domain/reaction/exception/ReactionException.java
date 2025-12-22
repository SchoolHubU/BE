package com.shu.backend.domain.reaction.exception;


import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class ReactionException extends GeneralException {
  public ReactionException(BaseErrorCode code) {
    super(code);
  }
}
