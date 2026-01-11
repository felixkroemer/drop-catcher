package com.felixkroemer.analysis;

import com.felixkroemer.analysis.result.AnalysisResult;
import java.io.File;

public interface FileAnalyzer {
  AnalysisResult analyze(File file);
}
