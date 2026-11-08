package com.aicodereviewer.health;

import com.aicodereviewer.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Spring Boot {@link HealthIndicator} that reports operational health of the
 * Google Gemini API integration without leaking sensitive credentials.
 */
@Component
public class GeminiApiHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(GeminiApiHealthIndicator.class);

    private final GeminiProperties geminiProperties;

    public GeminiApiHealthIndicator(GeminiProperties geminiProperties) {
        this.geminiProperties = geminiProperties;
    }

    @Override
    public Health health() {
        String apiKey = geminiProperties.getKey();
        String model = geminiProperties.getModel();
        String url = geminiProperties.getUrl();

        if (apiKey == null || apiKey.isBlank() || "your-api-key-here".equalsIgnoreCase(apiKey.trim())) {
            log.info("Gemini API Health check: API key not configured, operating in intelligent simulation mode");
            return Health.up()
                    .withDetail("provider", "Google Gemini")
                    .withDetail("mode", "LOCAL_SIMULATION")
                    .withDetail("model", model)
                    .withDetail("url", url)
                    .withDetail("apiKeyConfigured", false)
                    .withDetail("notice", "Zero-setup local analysis active. Set GEMINI_API_KEY environment variable to enable live Gemini 2.5 Flash LLM calls.")
                    .build();
        }

        // Mask the API key for safe telemetry exposure (never leak raw keys in /actuator/health)
        String maskedKey = maskKey(apiKey);

        return Health.up()
                .withDetail("provider", "Google Gemini")
                .withDetail("model", model)
                .withDetail("url", url)
                .withDetail("apiKeyConfigured", true)
                .withDetail("apiKey", maskedKey)
                .withDetail("connectTimeoutMs", geminiProperties.getConnectTimeout())
                .withDetail("readTimeoutMs", geminiProperties.getReadTimeout())
                .build();
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) {
            return "********";
        }
        return "..." + key.substring(key.length() - 4);
    }
}
