package com.aicodereviewer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI Code Review Assistant — Application Entry Point.
 *
 * <p>This application provides an AI-powered code review workflow where users submit
 * source code or Git unified diffs, receive structured feedback from Google Gemini,
 * and can accept or reject each suggestion through a human-in-the-loop review process.</p>
 *
 * <p>Retry support is activated via {@link com.aicodereviewer.config.RetryConfig}.</p>
 */
@SpringBootApplication
public class AiCodeReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeReviewApplication.class, args);
    }
}
