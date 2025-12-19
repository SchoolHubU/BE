package com.shu.backend.domain.chatroomuser.exception;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class ChatRoomUserException extends GeneralException {

  public ChatRoomUserException(BaseErrorCode code) {
    super(code);
  }
}
