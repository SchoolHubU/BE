package com.shu.backend.domain.report.exception;


import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.exception.GeneralException;

public class ReportException extends GeneralException {
  public ReportException(BaseErrorCode code) {
    super(code);
  }
}
