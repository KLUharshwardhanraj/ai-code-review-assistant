package com.aicodereviewer.exception;

import com.aicodereviewer.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler providing standardized {@link ApiErrorResponse} structures
 * across all REST endpoints.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles missing entities (ReviewSession or ReviewFeedback).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Handles invalid state or business logic decisions.
     */
    @ExceptionHandler(InvalidDecisionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidDecision(
            InvalidDecisionException ex, HttpServletRequest request) {
        log.warn("Invalid decision: {}", ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles Spring Bean Validation errors (@Valid on request bodies).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Validation failed for {}: {}", request.getRequestURI(), errors);

        ApiErrorResponse body = ApiErrorResponse.ofValidation(
                "Request validation failed. Check field errors for details.",
                request.getRequestURI(),
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles Jakarta constraint violations (e.g. query param validation).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage()));

        log.warn("Constraint violation: {}", errors);
        ApiErrorResponse body = ApiErrorResponse.ofValidation(
                "Constraint validation failed.",
                request.getRequestURI(),
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles illegal arguments or business logic violations.
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> handleIllegalStateOrArgument(
            RuntimeException ex, HttpServletRequest request) {
        log.warn("Business rule violation: {}", ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles Gemini-specific API or response parsing exceptions.
     */
    @ExceptionHandler({GeminiApiException.class, GeminiParsingException.class})
    public ResponseEntity<ApiErrorResponse> handleGeminiExceptions(
            RuntimeException ex, HttpServletRequest request) {
        log.error("Gemini AI service error: {}", ex.getMessage(), ex);
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_GATEWAY.value(),
                "Bad Gateway - AI Service Error",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    /**
     * Handles upstream HTTP client communication errors (e.g. Gemini 4xx/5xx).
     */
    @ExceptionHandler({org.springframework.web.client.HttpStatusCodeException.class, org.springframework.web.client.RestClientException.class})
    public ResponseEntity<ApiErrorResponse> handleRestClientExceptions(
            Exception ex, HttpServletRequest request) {
        log.error("Upstream HTTP service communication error: {}", ex.getMessage(), ex);
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_GATEWAY.value(),
                "Bad Gateway - Upstream API Communication Error",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    /**
     * Catch-all fallback for unexpected runtime errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception processing request to {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected internal error occurred. Please try again later or contact support.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
