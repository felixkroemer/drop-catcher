package com.felixkroemer.common;

import org.slf4j.helpers.MessageFormatter;

public class BaseException extends RuntimeException {

  private final ErrorCode code;

  public BaseException(ErrorCode errorCode, String message, Throwable e) {
    super(message, e);
    this.code = errorCode;
  }

  public BaseException(ErrorCode errorCode, String message, Object param1, Throwable e) {
    super(MessageFormatter.arrayFormat(message, new Object[] {param1}).getMessage(), e);
    this.code = errorCode;
  }

  public ErrorCode getCode() {
    return code;
  }
}
