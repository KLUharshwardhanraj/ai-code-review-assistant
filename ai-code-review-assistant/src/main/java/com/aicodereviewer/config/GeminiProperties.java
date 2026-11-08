package com.aicodereviewer.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Externalized configuration for the Google Gemini API integration.
 */
@ConfigurationProperties(prefix = "gemini.api")
@Validated
public class GeminiProperties {

    @NotBlank(message = "Gemini API key must not be blank. Set GEMINI_API_KEY env variable.")
    private String key;

    @NotBlank(message = "Gemini model must not be blank.")
    private String model = "gemini-2.5-flash";

    @NotBlank(message = "Gemini API base URL must not be blank.")
    private String url = "https://generativelanguage.googleapis.com/v1beta/models";

    private int connectTimeout = 5000;
    private int readTimeout = 25000;

    public GeminiProperties() {}

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}
