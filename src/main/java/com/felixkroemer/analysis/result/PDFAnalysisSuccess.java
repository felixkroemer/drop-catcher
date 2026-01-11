package com.felixkroemer.analysis.result;

import java.time.LocalDateTime;

public record PDFAnalysisSuccess(
    String filePath,
    String fileName,
    long fileSize,
    LocalDateTime analyzedAt,
    int pageCount,
    String analyzedName)
    implements AnalysisSuccess {

  @Override
  public String getAnalyzedName() {
    return analyzedName;
  }
}
