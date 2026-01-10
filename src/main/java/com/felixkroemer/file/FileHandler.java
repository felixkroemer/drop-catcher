package com.felixkroemer.file;

import com.felixkroemer.analyzer.FileAnalyzer;
import com.felixkroemer.analyzer.PDFAnalyzer;
import com.felixkroemer.analyzer.result.AnalysisFailure;
import com.felixkroemer.analyzer.result.AnalysisResult;
import com.felixkroemer.analyzer.result.AnalysisSuccess;
import com.felixkroemer.config.ConfigurationManager;
import com.felixkroemer.file.error.FileHandlingFailedException;
import com.felixkroemer.file.error.FileMoveException;
import com.felixkroemer.file.error.StabilityChecksExceededException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class FileHandler {

  private static final int MAX_CHECKS = 20;
  private static final int CHECK_INTERVAL_MS = 100;
  private static final int MAX_INTERVAL_MS = CHECK_INTERVAL_MS * 10;

  private final ConfigurationManager configurationManager;
  private final SessionFactory sessionFactory;

  private final Map<String, FileAnalyzer> analyzers;

  @Inject
  public FileHandler(
      ConfigurationManager configurationManager,
      PDFAnalyzer pdfAnalyzer,
      SessionFactory sessionFactory) {
    this.configurationManager = configurationManager;
    this.sessionFactory = sessionFactory;

    this.analyzers = Map.of("pdf", pdfAnalyzer);
  }

  public void handle(Path inputFilePath) {
    waitForFileStability(inputFilePath);

    String extension = extractFileExtension(inputFilePath);
    Optional<AnalysisResult> analysisResult = analyzeFile(inputFilePath, extension);

    analysisResult.ifPresent(
        result -> {
          switch (result) {
            case AnalysisSuccess success -> moveFile(success.getAnalyzedName(), inputFilePath);
            case AnalysisFailure failure ->
                log.error("File analysis failed: {}", failure.reason(), failure.cause());
          }
        });
  }

  private Optional<AnalysisResult> analyzeFile(Path filePath, String extension) {
    var analyzer = analyzers.get(extension.toLowerCase());

    if (analyzer == null) {
      log.info("No analyzer for extension: {}. Skipping analysis", extension);
      return Optional.empty();
    }

    return Optional.of(analyzer.analyze(filePath.toFile()));
  }

  private static @NonNull String extractFileExtension(Path inputFilePath) {
    String extension = "";
    String fileName = inputFilePath.getFileName().toString();
    int lastDot = fileName.lastIndexOf('.');
    if (lastDot > 0 && lastDot < fileName.length() - 1) {
      extension = fileName.substring(lastDot + 1);
    }
    return extension;
  }

  private void moveFile(String newFileName, Path inputFilePath) {
    try {
      var outputDir = configurationManager.getOutputDir();
      Path outputPath = outputDir.resolve(newFileName);
      Files.copy(inputFilePath, outputPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      throw new FileMoveException(
          "Failed to copy file to the output directory " + inputFilePath, e);
    }
  }

  private void waitForFileStability(Path inputFilePath) {
    int totalChecks = 0;
    int constantChecks = 0;
    var prevSize = -1L;
    long currentInterval = CHECK_INTERVAL_MS;
    while (true) {
      if (totalChecks == MAX_CHECKS) {
        throw new StabilityChecksExceededException(
            "Total checks exceeded for file: {}", inputFilePath);
      }
      try {
        long fileSize = Files.size(inputFilePath);
        if (prevSize != fileSize) {
          constantChecks = 0;
          prevSize = fileSize;
          currentInterval = CHECK_INTERVAL_MS;
        } else {
          constantChecks++;
          if (constantChecks >= 3) {
            break;
          }
          currentInterval = Math.min((long) (currentInterval * 1.5), MAX_INTERVAL_MS);
        }
        totalChecks++;
        log.info("Sleep with interval {}", currentInterval);
        Thread.sleep(currentInterval);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt(); // Restore interrupt status
        throw new FileHandlingFailedException(
            "File stability check interrupted for: {}", inputFilePath, e);
      } catch (IOException e) {
        throw new FileHandlingFailedException(
            "File stability check interrupted for: {}", inputFilePath, e);
      }
    }
    log.info("File stable: {}", inputFilePath);
  }
}
