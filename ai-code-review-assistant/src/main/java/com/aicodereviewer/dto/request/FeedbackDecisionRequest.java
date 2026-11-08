package com.aicodereviewer.dto.request;

import com.aicodereviewer.entity.enums.FeedbackStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for recording a human decision (Accept or Reject) on a feedback item.
 *
 * <p>Only {@link FeedbackStatus#ACCEPTED} and {@link FeedbackStatus#REJECTED} are permitted.
 * The server rejects {@code PENDING} or null values.</p>
 *
 * @param status the human decision on the feedback item
 */
@Schema(description = "Payload for accepting or rejecting an AI code review feedback item")
public record FeedbackDecisionRequest(
        @Schema(description = "Human decision: ACCEPTED or REJECTED", example = "ACCEPTED", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Decision status must not be null and must be either ACCEPTED or REJECTED")
        FeedbackStatus status
) {
}
