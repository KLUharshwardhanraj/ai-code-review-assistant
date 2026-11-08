package com.aicodereviewer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Activates Spring Retry AOP support for the application.
 *
 * <p>This enables {@code @Retryable} and {@code @Recover} annotations on
 * service beans. The Gemini service uses these to automatically retry
 * transient failures (5xx, 429, timeouts) with exponential backoff.</p>
 *
 * <p>Extracted into its own configuration class (rather than placing
 * {@code @EnableRetry} on the main application class) to keep cross-cutting
 * concerns separated and to allow selective inclusion in test slices.</p>
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
