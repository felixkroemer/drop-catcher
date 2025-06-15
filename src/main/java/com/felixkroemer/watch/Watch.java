package com.felixkroemer.watch;

import com.felixkroemer.FileHandler;
import com.felixkroemer.config.ConfigurationManager;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.*;

import static com.felixkroemer.config.ConfigurationManager.INPUT_DIRECTORY;

@Slf4j
public class Watch {

    private final ConfigurationManager configurationManager;
    private final WatchService watcher;
    private final FileHandler fileHandler;

    @Inject
    public Watch(ConfigurationManager configurationManager, FileHandler fileHandler) {
        this.configurationManager = configurationManager;
        this.fileHandler = fileHandler;
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
                    fileHandler.handle(inputDir.resolve(filename));
                }
            }
            key.reset();
        }
    }

}
