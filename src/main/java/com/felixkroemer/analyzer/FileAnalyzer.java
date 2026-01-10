package com.felixkroemer.analyzer;

import com.felixkroemer.analyzer.result.AnalysisResult;
import java.io.File;

public interface FileAnalyzer {
  AnalysisResult analyze(File file);
}
