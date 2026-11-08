package com.aicodereviewer.exception;

/**
 * Thrown when the Gemini API returns an unexpected or invalid response.
 *
 * <p>This covers non-STOP finish reasons (e.g., {@code SAFETY}, {@code MAX_TOKENS}),
 * empty candidate lists, or any structural response anomaly that prevents
 * successful feedback extraction.</p>
 *
 * <p>This is a <strong>deterministic</strong> failure — retrying with the same
 * input will produce the same result. Therefore, this exception is intentionally
 * excluded from the {@code @Retryable} include list in {@code GeminiService}.</p>
 */
public class GeminiApiException extends RuntimeException {

    public GeminiApiException(String message) {
        super(message);
    }

    public GeminiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
