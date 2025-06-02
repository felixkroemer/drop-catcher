package com.felixkroemer.config;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigurationManager {

    public static final String INPUT_DIRECTORY = "inputDir";
    public static final String OUTPUT_DIRECTORY = "outputDir";

    private static final String CONFIG_FILENAME = "config.yml";

    private Configuration config;
    private FileBasedConfigurationBuilder<YAMLConfiguration> builder;

    public ConfigurationManager() {
        try {
            initializeConfiguration();
        } catch (Exception e) {
            throw new RuntimeException("Error initializing config", e);
        }
    }

    private File getConfigFile() throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        Path configDir;

        if (os.contains("win")) {
            configDir = Paths.get(System.getenv("APPDATA"), "drop-catcher");
        } else {
            configDir = Paths.get(System.getProperty("user.home"), ".config", "drop-catcher");
        }

        Files.createDirectories(configDir);

        return configDir.resolve(CONFIG_FILENAME).toFile();
    }

    private void initializeConfiguration() throws ConfigurationException, IOException {
        Parameters params = new Parameters();

        FileBasedConfigurationBuilder<YAMLConfiguration> builderDefaults = new FileBasedConfigurationBuilder<>(YAMLConfiguration.class).configure(params.properties().setURL(getClass().getClassLoader().getResource(CONFIG_FILENAME)));

        builder = new FileBasedConfigurationBuilder<>(YAMLConfiguration.class).configure(params.fileBased().setFile(getConfigFile()));

        CompositeConfiguration cc = new CompositeConfiguration();
        cc.addConfiguration(builder.getConfiguration());
        cc.addConfiguration(builderDefaults.getConfiguration());

        config = cc;
    }

    public String getString(String key) {
        return config.getString(key);
    }

    public void setProperty(String key, Object value) {
        config.setProperty(key, value);
    }

    public void save() throws ConfigurationException {
        builder.save();
    }
}