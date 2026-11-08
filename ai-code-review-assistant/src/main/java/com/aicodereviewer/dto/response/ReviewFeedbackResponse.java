package com.aicodereviewer.dto.response;

import com.aicodereviewer.entity.ReviewFeedback;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

/**
 * Response DTO representing an individual review feedback item.
 *
 * @param id              unique ID of the feedback item
 * @param lineNumber      line number in source code (null if not line-specific)
 * @param category        category (BUG, EDGE_CASE, READABILITY, SECURITY, etc.)
 * @param severity        severity level (CRITICAL, HIGH, WARNING, MEDIUM, LOW, INFO)
 * @param suggestion      detailed suggestion / explanation
 * @param replacementCode suggested code replacement snippet (null if none)
 * @param status          resolution state (PENDING, ACCEPTED, REJECTED)
 * @param createdAt       creation timestamp
 */
@Schema(description = "Details of an individual code review feedback item")
public record ReviewFeedbackResponse(
        @Schema(description = "Feedback unique identifier", example = "42")
        Long id,

        @Schema(description = "Line number referenced in the source code", example = "15")
        @JsonProperty("line_number")
        Integer lineNumber,

        @Schema(description = "Feedback category", example = "BUG")
        String category,

        @Schema(description = "Severity level", example = "CRITICAL")
        String severity,

        @Schema(description = "Actionable suggestion or explanation", example = "Potential NullPointerException when accessing order.getItems() without null check.")
        String suggestion,

        @Schema(description = "Concrete replacement code snippet", example = "if (order != null && order.getItems() != null) { ... }")
        @JsonProperty("replacement_code")
        String replacementCode,

        @Schema(description = "Human review decision status", example = "PENDING")
        String status,

        @Schema(description = "Timestamp when feedback was generated")
        @JsonProperty("created_at")
        String createdAt
) {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Maps a JPA {@link ReviewFeedback} entity to this response DTO.
     */
    public static ReviewFeedbackResponse fromEntity(ReviewFeedback entity) {
        if (entity == null) {
            return null;
        }

        String formattedDate = entity.getCreatedAt() != null
                ? entity.getCreatedAt().format(ISO_FORMATTER)
                : null;

        return new ReviewFeedbackResponse(
                entity.getId(),
                entity.getLineStart(),
                entity.getCategory() != null ? entity.getCategory().name() : "OTHER",
                entity.getSeverity() != null ? entity.getSeverity().name() : "INFO",
                entity.getDescription() != null ? entity.getDescription() : entity.getTitle(),
                entity.getSuggestedCode(),
                entity.getResolution() != null ? entity.getResolution().name() : "PENDING",
                formattedDate
        );
    }
}