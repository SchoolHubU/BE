package com.shu.backend.domain.notification.exception;


import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class NotificationException extends GeneralException {
  public NotificationException(BaseErrorCode code) {
    super(code);
  }
}
