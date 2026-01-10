package com.felixkroemer.analyzer.result;

import org.jetbrains.annotations.Nullable;

public record AnalysisFailure(String reason, @Nullable Throwable cause) implements AnalysisResult {}
