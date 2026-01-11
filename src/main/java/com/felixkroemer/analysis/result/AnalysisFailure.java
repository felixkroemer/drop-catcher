package com.felixkroemer.analysis.result;

import com.felixkroemer.common.ErrorCode;
import org.jetbrains.annotations.Nullable;

public record AnalysisFailure(String reason, ErrorCode code, @Nullable Throwable cause) implements AnalysisResult {}
