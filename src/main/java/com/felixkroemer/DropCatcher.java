package com.felixkroemer;

import com.felixkroemer.config.ConfigurationManager;
import com.felixkroemer.watch.Watch;

public class DropCatcher {
    public static void main(String[] args) {
        ConfigurationManager configurationManager = new ConfigurationManager();
        Watch watch = new Watch(configurationManager);
        watch.watch();
    }
}