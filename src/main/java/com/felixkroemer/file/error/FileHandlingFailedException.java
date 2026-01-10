package com.felixkroemer.file.error;

import com.felixkroemer.common.BaseException;
import com.felixkroemer.common.ErrorCode;

public class FileHandlingFailedException extends BaseException {

  public FileHandlingFailedException(ErrorCode errorCode, String message, Throwable e) {
    super(errorCode, message, e);
  }

  public FileHandlingFailedException(String message, Object param1, Throwable e) {
    super(ErrorCode.FILE_HANDLING_FAILED, message, param1, e);
  }

  public FileHandlingFailedException(
      ErrorCode errorCode, String message, Object param1, Throwable e) {
    super(errorCode, message, param1, e);
  }
}
