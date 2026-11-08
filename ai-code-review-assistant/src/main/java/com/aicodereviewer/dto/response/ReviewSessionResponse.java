package com.aicodereviewer.dto.response;

import com.aicodereviewer.entity.ReviewSession;
import com.aicodereviewer.entity.enums.FeedbackResolution;
import com.aicodereviewer.entity.enums.ReviewStatus;
import com.aicodereviewer.entity.enums.SubmissionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Schema(description = "Complete review session details including code and all feedback items")
public record ReviewSessionResponse(
        @Schema(description = "Session identifier", example = "1")
        @JsonProperty("session_id")
        Long sessionId,

        @Schema(description = "Session title", example = "Payment Gateway Controller Review")
        String title,

        @Schema(description = "Programming language", example = "java")
        String language,

        @Schema(description = "Submission type", example = "SNIPPET")
        @JsonProperty("submission_type")
        SubmissionType submissionType,

        @Schema(description = "Raw submitted code or diff")
        @JsonProperty("code_content")
        String codeContent,

        @Schema(description = "Session review status", example = "IN_REVIEW")
        ReviewStatus status,

        @Schema(description = "Gemini model version used", example = "gemini-2.5-flash")
        @JsonProperty("gemini_model")
        String geminiModel,

        @Schema(description = "Gemini API round-trip duration in milliseconds", example = "2340")
        @JsonProperty("gemini_request_duration_ms")
        Long geminiRequestDurationMs,

        @Schema(description = "Creation timestamp")
        @JsonProperty("created_at")
        String createdAt,

        @Schema(description = "List of feedback items")
        List<ReviewFeedbackResponse> feedbacks,

        @Schema(description = "Total feedback count", example = "5")
        @JsonProperty("total_count")
        int totalCount,

        @Schema(description = "Accepted feedback count", example = "2")
        @JsonProperty("accepted_count")
        int acceptedCount,

        @Schema(description = "Rejected feedback count", example = "1")
        @JsonProperty("rejected_count")
        int rejectedCount,

        @Schema(description = "Pending feedback count", example = "2")
        @JsonProperty("pending_count")
        int pendingCount
) {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static ReviewSessionResponse fromEntity(ReviewSession session) {
        if (session == null) {
            return null;
        }

        List<ReviewFeedbackResponse> feedbackResponses = session.getFeedbackItems() != null
                ? session.getFeedbackItems().stream()
                .map(ReviewFeedbackResponse::fromEntity)
                .toList()
                : Collections.emptyList();

        int total = feedbackResponses.size();
        int accepted = (int) feedbackResponses.stream()
                .filter(f -> f.status() != null && FeedbackResolution.ACCEPTED.name().equalsIgnoreCase(f.status()))
                .count();
        int rejected = (int) feedbackResponses.stream()
                .filter(f -> f.status() != null && FeedbackResolution.REJECTED.name().equalsIgnoreCase(f.status()))
                .count();
        int pending = total - (accepted + rejected);

        String formattedDate = session.getCreatedAt() != null
                ? session.getCreatedAt().format(ISO_FORMATTER)
                : null;

        return new ReviewSessionResponse(
                session.getId(),
                session.getTitle(),
                session.getLanguage(),
                session.getSubmissionType(),
                session.getSourceCode(),
                session.getStatus(),
                session.getGeminiModel(),
                session.getGeminiRequestDurationMs(),
                formattedDate,
                feedbackResponses,
                total,
                accepted,
                rejected,
                pending
        );
    }
}