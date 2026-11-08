package com.aicodereviewer.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Standardized error response body returned by {@link com.aicodereviewer.exception.GlobalExceptionHandler}
 * for all error scenarios (validation, not-found, server errors).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Factory for simple (non-validation) errors.
     */
    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(LocalDateTime.now().format(ISO_FORMATTER), status, error, message, path, null);
    }

    /**
     * Factory for validation errors with field-level details.
     */
    public static ApiErrorResponse ofValidation(String message, String path, Map<String, String> fieldErrors) {
        return new ApiErrorResponse(LocalDateTime.now().format(ISO_FORMATTER), 400, "Bad Request", message, path, fieldErrors);
    }
}
