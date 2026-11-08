package com.aicodereviewer.exception;

/**
 * Thrown when a requested entity (session or feedback item) does not exist.
 * Mapped to HTTP 404 Not Found by {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(String.format("%s not found with id: %s", resourceName, resourceId));
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public Object getResourceId() {
        return resourceId;
    }
}
