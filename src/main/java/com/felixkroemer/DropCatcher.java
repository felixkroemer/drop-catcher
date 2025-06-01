package com.felixkroemer;

import com.felixkroemer.config.ConfigurationManager;

public class DropCatcher {
    public static void main(String[] args) {
        ConfigurationManager configurationManager = new ConfigurationManager();
        System.out.println(configurationManager.getString("nest.test"));
    }
}