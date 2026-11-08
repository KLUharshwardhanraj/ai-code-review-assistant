package com.aicodereviewer.dto.response;

import com.aicodereviewer.entity.ReviewSession;
import com.aicodereviewer.entity.enums.ReviewStatus;
import com.aicodereviewer.entity.enums.SubmissionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

/**
 * Compact summary representation of a review session for list/dashboard views.
 *
 * @param sessionId                unique ID
 * @param title                    session title
 * @param language                 programming language
 * @param submissionType           SNIPPET or DIFF
 * @param status                   lifecycle status
 * @param totalFeedbackCount       total items
 * @param acceptedCount            accepted items
 * @param rejectedCount            rejected items
 * @param pendingCount             items awaiting decision
 * @param geminiModel              Gemini model used
 * @param geminiRequestDurationMs call duration in ms
 * @param createdAt                session creation timestamp
 */
@Schema(description = "Compact summary of a review session for history and dashboard listings")
public record ReviewSessionSummaryResponse(
        @Schema(description = "Session identifier", example = "1")
        @JsonProperty("sessionId")
        Long sessionId,

        @Schema(description = "Session title", example = "Payment Gateway Controller Review")
        String title,

        @Schema(description = "Programming language", example = "java")
        String language,

        @Schema(description = "Submission type", example = "SNIPPET")
        @JsonProperty("submission_type")
        SubmissionType submissionType,

        @Schema(description = "Review status", example = "COMPLETED")
        ReviewStatus status,

        @Schema(description = "Total feedback count", example = "5")
        @JsonProperty("total_feedback_count")
        int totalFeedbackCount,

        @Schema(description = "Accepted feedback count", example = "2")
        @JsonProperty("accepted_count")
        int acceptedCount,

        @Schema(description = "Rejected feedback count", example = "1")
        @JsonProperty("rejected_count")
        int rejectedCount,

        @Schema(description = "Pending feedback count", example = "2")
        @JsonProperty("pending_count")
        int pendingCount,

        @Schema(description = "Gemini model version used", example = "gemini-2.5-flash")
        @JsonProperty("gemini_model")
        String geminiModel,

        @Schema(description = "Gemini API latency in milliseconds", example = "1850")
        @JsonProperty("gemini_request_duration_ms")
        Long geminiRequestDurationMs,

        @Schema(description = "Creation timestamp")
        @JsonProperty("created_at")
        String createdAt
) {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Maps a JPA {@link ReviewSession} to a lightweight summary DTO.
     */
    public static ReviewSessionSummaryResponse fromEntity(ReviewSession session) {
        if (session == null) {
            return null;
        }

        int total = session.getTotalFeedbackCount() != null ? session.getTotalFeedbackCount() : 0;
        int accepted = session.getAcceptedCount() != null ? session.getAcceptedCount() : 0;
        int rejected = session.getRejectedCount() != null ? session.getRejectedCount() : 0;
        int pending = Math.max(0, total - (accepted + rejected));

        String formattedDate = session.getCreatedAt() != null
                ? session.getCreatedAt().format(ISO_FORMATTER)
                : null;

        return new ReviewSessionSummaryResponse(
                session.getId(),
                session.getTitle(),
                session.getLanguage(),
                session.getSubmissionType(),
                session.getStatus(),
                total,
                accepted,
                rejected,
                pending,
                session.getGeminiModel(),
                session.getGeminiRequestDurationMs(),
                formattedDate
        );
    }
}