package com.felixkroemer.analysis;

import com.felixkroemer.analysis.ai.OAIService;
import com.felixkroemer.analysis.result.AnalysisFailure;
import com.felixkroemer.analysis.result.AnalysisIncomplete;
import com.felixkroemer.analysis.result.AnalysisResult;
import com.felixkroemer.analysis.result.PDFAnalysisSuccess;
import com.felixkroemer.common.BaseException;
import com.felixkroemer.common.ErrorCode;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
public class PDFAnalyzer implements FileAnalyzer {

  private final OAIService oaiService;

  @Inject
  public PDFAnalyzer(OAIService oaiService) {
    this.oaiService = oaiService;
  }

  public AnalysisResult analyze(File f) {
    try (var is = new FileInputStream(f)) {
      PdfReader reader = new PdfReader(is);
      var analyzableContent = getContentForNameAnalysis(reader);
      if (analyzableContent.isEmpty()) {
        return new AnalysisIncomplete("PDF contains no text to analyze: " + f.getAbsolutePath());
      }
      var analyzedFileName = oaiService.analyzeFileName(analyzableContent.get());
      log.info("Analyzed file name for file {}: {}", f.getAbsolutePath(), analyzedFileName);
      return new PDFAnalysisSuccess(
          f.getAbsolutePath(),
          f.getName(),
          f.getTotalSpace(),
          LocalDateTime.now(),
          reader.getNumberOfPages(),
          analyzedFileName);
    } catch (BaseException e) {
      return new AnalysisFailure(e.getMessage(), e.getCode(), e);
    } catch (Exception e) {
      var be = new BaseException(ErrorCode.ANALYSIS_FAILED, "Failed to analyze file: {}", e);
      return new AnalysisFailure(be.getMessage(), be.getCode(), be);
    }
  }

  private Optional<String> getContentForNameAnalysis(PdfReader reader) throws IOException {
    PdfTextExtractor pdfTextExtractor = new PdfTextExtractor(reader);
    var text = pdfTextExtractor.getTextFromPage(1);
    return Optional.of(text).filter(s -> !s.isBlank());
  }
}
