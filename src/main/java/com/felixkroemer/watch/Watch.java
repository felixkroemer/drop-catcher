package com.felixkroemer.watch;

import com.felixkroemer.config.ConfigurationManager;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.felixkroemer.config.ConfigurationManager.INPUT_DIRECTORY;
import static com.felixkroemer.config.ConfigurationManager.OUTPUT_DIRECTORY;

@Slf4j
public class Watch {

    private final ConfigurationManager configurationManager;
    private final WatchService watcher;

    public Watch(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
        try {
            this.watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create WatchService", e);
        }
    }

    public void watch() {
        Path inputDir = Paths.get(configurationManager.getString(INPUT_DIRECTORY));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown signal received, stopping watcher...");
            try {
                watcher.close();
            } catch (IOException e) {
                log.error("Error closing watcher", e);
            }
        }));

        try {
            inputDir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Error registering watch", e);
        }

        while (true) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (ClosedWatchServiceException e) {
                log.info("Watch service closed, shutting down");
                break;
            } catch (InterruptedException e) {
                log.error("Watcher interrupted", e);
                continue;
            }
            for (WatchEvent<?> event : key.pollEvents()) {
                Path filename = (Path) event.context();
                if (!filename.toString().endsWith(".swp") && !filename.toString().endsWith(".part") && !filename.toString().startsWith(".")) {
                    try {
                        Path inputPath = inputDir.resolve(filename);
                        Path outputDir = Path.of(configurationManager.getString(OUTPUT_DIRECTORY));
                        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));
                        Path outputPath = outputDir.resolve(timestamp);
                        Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        log.info("Error during file copy", e);
                        break;
                    }
                }
            }
            key.reset();
        }
    }

}
