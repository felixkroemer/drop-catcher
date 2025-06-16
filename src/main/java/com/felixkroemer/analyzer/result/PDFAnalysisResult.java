package com.felixkroemer.analyzer.result;

import java.time.LocalDateTime;

public record PDFAnalysisResult(String filePath,
                                String fileName,
                                long fileSize,
                                LocalDateTime analyzedAt,
                                int pageCount,
                                String analyzedName) implements AnalysisResult {

    @Override
    public String getAnalyzedName() {
        return analyzedName;
    }
}
