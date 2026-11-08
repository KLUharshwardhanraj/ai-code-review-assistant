package com.aicodereviewer.dto.request;

import com.aicodereviewer.entity.enums.SubmissionType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for submitting code or a Git diff for AI review.
 *
 * @param code           source code snippet or unified Git diff (max 50,000 characters)
 * @param language       programming language (e.g. "java", "python", "typescript")
 * @param submissionType SNIPPET or DIFF (defaults to SNIPPET if omitted)
 * @param title          optional descriptive title for the review session
 */
@Schema(description = "Payload for submitting source code or a Git diff for automated AI code review")
public record SubmitCodeRequest(
        @Schema(description = "Source code or Git unified diff content", example = "public class Example { ... }", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Source code must not be blank")
        @Size(max = 50000, message = "Code content must not exceed 50,000 characters")
        String code,

        @Schema(description = "Programming language of the code", example = "java", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Language must not be blank")
        String language,

        @Schema(description = "Submission type: SNIPPET or DIFF", example = "SNIPPET", defaultValue = "SNIPPET")
        @JsonProperty("submission_type")
        @JsonAlias({"submissionType", "submission_mode"})
        SubmissionType submissionType,

        @Schema(description = "Optional custom session title", example = "Payment Gateway Controller Review")
        @JsonProperty("title")
        @JsonAlias({"sessionTitle", "session_title"})
        String title
) {

    /**
     * Compact constructor normalizing language to lowercase and defaulting submissionType.
     */
    public SubmitCodeRequest {
        if (language != null) {
            language = language.trim().toLowerCase();
        }
        if (submissionType == null) {
            submissionType = SubmissionType.SNIPPET;
        }
    }
}