package com.felixkroemer;

import com.felixkroemer.config.ConfigurationManager;
import com.felixkroemer.watch.Watch;

public class DropCatcher {
    public static void main(String[] args) {
        ConfigurationManager configurationManager = new ConfigurationManager();
        FileHandler fileHandler = new FileHandler(configurationManager);
        Watch watch = new Watch(configurationManager, fileHandler);
        watch.watch();
    }
}