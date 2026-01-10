package com.felixkroemer.file.error;

import com.felixkroemer.common.ErrorCode;

public class StabilityChecksExceededException extends FileHandlingFailedException {
  public StabilityChecksExceededException(String message, Object param1) {
    super(ErrorCode.STABILITY_CHECKS_EXCEEDED, message, param1, null);
  }
}
