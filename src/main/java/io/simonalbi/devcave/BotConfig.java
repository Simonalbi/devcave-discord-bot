package io.simonalbi.devcave;

import java.io.IOException;
import java.util.Properties;

public class BotConfig {
    private final String propertiesPath;
    private final Properties properties;

    public BotConfig(String propertiesPath) throws IOException {
        this.propertiesPath = propertiesPath;
        this.properties = this.getApplicationProperties();
    }

    private Properties getApplicationProperties() throws IOException {
        Properties applicationProperties = new Properties();

        try (var input = Main.class.getClassLoader().getResourceAsStream(propertiesPath)) {
            if (input == null) {
                throw new IllegalStateException(propertiesPath + " not found!");
            }
            applicationProperties.load(input);
        }

        return applicationProperties;
    }

    public String get(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Cannot find property '" + key + "'  in '" + propertiesPath + "'");
        }
        return value;
    }

    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public long getLong(String key) {
        String value = get(key);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Property '" + key + "' in '" + propertiesPath + "' is not a valid long: " + value, e);
        }
    }

    public boolean getBoolean(String key) {
        String value = get(key);
        return Boolean.parseBoolean(value);
    }
}
