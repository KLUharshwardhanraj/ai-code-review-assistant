package com.aicodereviewer.exception;

/**
 * Thrown when the JSON text extracted from a Gemini response cannot be
 * deserialized into the expected {@code List<FeedbackItemDto>} structure.
 *
 * <p>This indicates either a Gemini output that violated its own schema
 * constraints, or an unexpected structural change in the API response.
 * Like {@link GeminiApiException}, this is a deterministic failure and
 * is excluded from retry logic.</p>
 */
public class GeminiParsingException extends RuntimeException {

    public GeminiParsingException(String message) {
        super(message);
    }

    public GeminiParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
