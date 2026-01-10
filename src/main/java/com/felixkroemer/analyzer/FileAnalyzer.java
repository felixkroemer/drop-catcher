package com.felixkroemer.analyzer;

import java.io.File;

public interface FileAnalyzer<T> {
  T analyze(File file);
}
