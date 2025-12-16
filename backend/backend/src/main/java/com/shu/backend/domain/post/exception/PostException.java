package com.shu.backend.domain.post.exception;


import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class PostException extends GeneralException {
  public PostException(BaseErrorCode code) {
    super(code);
  }
}
