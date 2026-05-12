package com.skycast.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AppConfig {

    private static final String API_KEY_NAME = "OPENWEATHER_API_KEY";
    private static final String LOCAL_PROPERTIES_FILE = "local.properties";

    private AppConfig() {
    }

    public static String getOpenWeatherApiKey() {
        String apiKeyFromEnv = System.getenv(API_KEY_NAME);
        if (isValid(apiKeyFromEnv)) {
            return apiKeyFromEnv.trim();
        }

        String apiKeyFromSystemProperty = System.getProperty(API_KEY_NAME);
        if (isValid(apiKeyFromSystemProperty)) {
            return apiKeyFromSystemProperty.trim();
        }

        String apiKeyFromLocalProperties = readApiKeyFromLocalProperties();
        if (isValid(apiKeyFromLocalProperties)) {
            return apiKeyFromLocalProperties.trim();
        }

        throw new IllegalStateException(
                "OpenWeather API key not found. Set environment variable OPENWEATHER_API_KEY " +
                "or create a local.properties file in the project root with:\n" +
                "OPENWEATHER_API_KEY=your_key_here"
        );
    }

    private static String readApiKeyFromLocalProperties() {
        Path path = Path.of(LOCAL_PROPERTIES_FILE);

        if (!Files.exists(path)) {
            return null;
        }

        Properties properties = new Properties();

        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
            return properties.getProperty(API_KEY_NAME);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read local.properties file.", e);
        }
    }

    private static boolean isValid(String value) {
        return value != null && !value.trim().isEmpty();
    }
}