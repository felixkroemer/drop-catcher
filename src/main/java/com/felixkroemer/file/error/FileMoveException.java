package com.felixkroemer.file.error;

import com.felixkroemer.common.ErrorCode;

public class FileMoveException extends FileHandlingFailedException {

  public FileMoveException(String message, Throwable e) {
    super(ErrorCode.FILE_MOVE_FAILED, message, e);
  }

}
