package com.felixkroemer.analysis.result;

public sealed interface AnalysisResult permits AnalysisSuccess, AnalysisIncomplete, AnalysisFailure {}
