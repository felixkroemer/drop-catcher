package com.felixkroemer;

import com.felixkroemer.config.ConfigurationManager;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.felixkroemer.config.ConfigurationManager.OUTPUT_DIRECTORY;

@Slf4j
public class FileHandler {

    private static final int MAX_CHECKS = 20;
    private static final int CHECK_INTERVAL_MS = 100;
    private static final int MAX_INTERVAL_MS = CHECK_INTERVAL_MS * 10;

    private final ConfigurationManager configurationManager;

    public FileHandler(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    public void handle(Path inputFilePath) {
        if (waitForFileStability(inputFilePath)) {
            Path outputDir = Path.of(configurationManager.getString(OUTPUT_DIRECTORY));
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));

            String extension = "";
            String fileName = inputFilePath.getFileName().toString();
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot > 0 && lastDot < fileName.length() - 1) {
                extension = fileName.substring(lastDot + 1);
            }
            Path outputPath = outputDir.resolve(timestamp + (!extension.isEmpty() ? "." + extension : ""));
            try {
                Files.copy(inputFilePath, outputPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                throw new RuntimeException("Failed to copy file to the output directory " + inputFilePath, e);
            }
        }
    }

    private boolean waitForFileStability(Path inputFilePath) {
        int totalChecks = 0;
        int constantChecks = 0;
        var prevSize = -1L;
        long currentInterval = CHECK_INTERVAL_MS;
        while (true) {
            if (totalChecks == MAX_CHECKS) {
                log.info("Total checks exceeded for file: {}", inputFilePath);
                return false;
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
                log.info("Sleep with interaval {}", currentInterval);
                Thread.sleep(currentInterval);
            } catch (InterruptedException e) {
                log.warn("File stability check interrupted for: {}", inputFilePath);
                Thread.currentThread().interrupt(); // Restore interrupt status
                return false;
            } catch (IOException e) {
                log.error("IO error during file stability check for {}: {}", inputFilePath, e.getMessage());
                return false;
            }
        }
        log.info("File stable: {}", inputFilePath);
        return true;
    }

}
