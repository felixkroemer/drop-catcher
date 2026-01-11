package com.felixkroemer.file;

import com.felixkroemer.analysis.FileAnalyzer;
import com.felixkroemer.analysis.PDFAnalyzer;
import com.felixkroemer.analysis.result.AnalysisFailure;
import com.felixkroemer.analysis.result.AnalysisIncomplete;
import com.felixkroemer.analysis.result.AnalysisResult;
import com.felixkroemer.analysis.result.AnalysisSuccess;
import com.felixkroemer.common.BaseException;
import com.felixkroemer.common.ErrorCode;
import com.felixkroemer.config.ConfigurationManager;
import com.felixkroemer.file.error.FileHandlingFailedException;
import com.felixkroemer.file.error.FileMoveException;
import com.felixkroemer.file.error.FileMoveStatus;
import com.felixkroemer.file.error.StabilityChecksExceededException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
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

    FileMoveEntity entity;
    try {
      entity = persistPendingEntity(inputFilePath);
    } catch (Exception e) {
      log.error("Failed to persist pending entity", e);
      return;
    }

    try {
      waitForFileStability(inputFilePath);

      String extension = extractFileExtension(inputFilePath);
      Optional<AnalysisResult> analysisResult = analyzeFile(inputFilePath, extension);

      analysisResult.ifPresentOrElse(
          result -> {
            switch (result) {
              case AnalysisSuccess success -> {
                var outputDir = configurationManager.getOutputDir();
                moveFile(success.getAnalyzedName(), inputFilePath, outputDir);
                entity.setStatus(FileMoveStatus.SUCCEEDED);
                entity.setMoveCompletedAt(LocalDateTime.now());
                entity.setTargetDirectory(outputDir.toString());
                entity.setTargetFileName(success.getAnalyzedName());
              }
              case AnalysisIncomplete incomplete -> {
                log.info("Analysis was incomplete: {}", incomplete.message());
                entity.setErrorMessage(incomplete.message());
                entity.setErrorCode(ErrorCode.ANALYSIS_INCOMPLETE);
                entity.setStatus(FileMoveStatus.MOVE_FAILED);
              }
              case AnalysisFailure failure -> {
                log.error("File analysis failed: {}", failure.reason(), failure.cause());
                entity.setErrorMessage(failure.reason());
                entity.setErrorCode(failure.code());
                entity.setStatus(FileMoveStatus.MOVE_FAILED);
              }
            }
          },
          () -> {
            entity.setErrorMessage("No analyzer for extension: " + extension);
            entity.setErrorCode(ErrorCode.NO_ANALYZER_AVAILABLE);
            entity.setStatus(FileMoveStatus.MOVE_FAILED);
          });
    } catch (BaseException e) {
      entity.setErrorCode(e.getCode());
      entity.setErrorMessage(e.getMessage());
      entity.setStatus(FileMoveStatus.MOVE_FAILED);
      throw e;
    } catch (Exception e) {
      entity.setErrorMessage(e.getMessage());
      entity.setStatus(FileMoveStatus.MOVE_FAILED_UNEXPECTED_ERROR);
      throw e;
    } finally {
      sessionFactory.inTransaction((session) -> session.merge(entity));
    }
  }

  private FileMoveEntity persistPendingEntity(Path inputFilePath) {
    BasicFileAttributes attrs;
    try {
      attrs = Files.readAttributes(inputFilePath, BasicFileAttributes.class);
    } catch (Exception e) {
      log.error("Could not retrieve file attributes: {}", inputFilePath, e);
      throw new RuntimeException(e);
    }
    FileMoveEntity entity =
        FileMoveEntity.builder()
            .sourceDirectory(inputFilePath.getParent().toString())
            .sourceFileName(inputFilePath.getFileName().toString())
            .fileSize(attrs.size())
            .fileHash("TODO")
            .status(FileMoveStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();
    return sessionFactory.fromTransaction(
        (session) -> {
          session.persist(entity);
          return entity;
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

  private void moveFile(String newFileName, Path inputFilePath, Path outputDir) {
    try {
      Path outputPath = outputDir.resolve(newFileName);
      Files.move(inputFilePath, outputPath, StandardCopyOption.REPLACE_EXISTING);
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
      } catch (Exception e) {
        if (e instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
        throw new FileHandlingFailedException(
            "Exception during file stability check for file: {}", inputFilePath, e);
      }
    }
    log.info("File stable: {}", inputFilePath);
  }
}
