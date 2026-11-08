package com.aicodereviewer.exception;

/**
 * Thrown when a feedback decision transition is invalid.
 *
 * <p>Examples: attempting to change an already-accepted item to rejected,
 * or re-deciding an item that has already been resolved.</p>
 *
 * <p>Mapped to HTTP 400 Bad Request by {@link GlobalExceptionHandler}.</p>
 */
public class InvalidDecisionException extends RuntimeException {

    public InvalidDecisionException(String message) {
        super(message);
    }
}
