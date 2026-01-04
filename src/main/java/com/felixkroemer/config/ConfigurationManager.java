package com.felixkroemer.config;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigurationManager {

    public static final String INPUT_DIRECTORY = "inputDir";
    public static final String OUTPUT_DIRECTORY = "outputDir";
    public static final String OAI_KEY = "oaiKey";
    public static final String LLM_MODEL = "llmModel";

    private static final String CONFIG_FILENAME = "application.properties";

    private Configuration config;
    private FileBasedConfigurationBuilder<PropertiesConfiguration> builder;

    @Inject
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

        FileBasedConfigurationBuilder<PropertiesConfiguration> builderDefaults = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class).configure(params.fileBased().setURL(getClass().getClassLoader().getResource(CONFIG_FILENAME)));
        builder = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class).configure(params.fileBased().setFile(getConfigFile()));

        CompositeConfiguration cc = new CompositeConfiguration();
        cc.addConfiguration(builder.getConfiguration());
        cc.addConfiguration(builderDefaults.getConfiguration());

        config = cc;
    }

    public Path getPath(String key) {
        String rawPath = config.getString(key);
        if (rawPath == null) {
            return null;
        }
        if (rawPath.startsWith("~" + File.separator) || rawPath.equals("~")) {
            return Paths.get(System.getProperty("user.home") + rawPath.substring(1))
                    .toAbsolutePath().normalize();
        }
        return Paths.get(rawPath).toAbsolutePath().normalize();
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