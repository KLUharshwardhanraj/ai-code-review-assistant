package com.aicodereviewer.controller;

import com.aicodereviewer.dto.request.FeedbackDecisionRequest;
import com.aicodereviewer.dto.request.SubmitCodeRequest;
import com.aicodereviewer.dto.response.ApiErrorResponse;
import com.aicodereviewer.dto.response.ReviewFeedbackResponse;
import com.aicodereviewer.dto.response.ReviewSessionResponse;
import com.aicodereviewer.dto.response.ReviewSessionSummaryResponse;
import com.aicodereviewer.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Primary REST Controller exposing the AI Code Review Assistant API.
 */
@RestController
@RequestMapping("/api/v1/reviews")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, maxAge = 3600)
@Tag(name = "Review Management", description = "Endpoints for AI code review submission, session retrieval, and feedback resolution")
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Submits a source code snippet or Git unified diff for automated AI code review.
     */
    @Operation(
            summary = "Submit code or Git diff for AI review",
            description = "Creates a new review session, analyzes the code with Google Gemini, and returns structured feedback items."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review session created and AI analysis completed successfully",
                    content = @Content(schema = @Schema(implementation = ReviewSessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or code length exceeded",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during analysis",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Upstream Gemini AI service error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/submit")
    public ResponseEntity<ReviewSessionResponse> submitCode(
            @Valid @RequestBody SubmitCodeRequest request) {
        log.info("Received code submission request for language: {}", request.language());
        ReviewSessionResponse response = reviewService.createReviewSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates the human review decision (ACCEPT or REJECT) for a feedback item.
     */
    @Operation(
            summary = "Accept or Reject a feedback suggestion",
            description = "Applies human-in-the-loop decision to a feedback card. Status can be ACCEPTED or REJECTED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback decision updated successfully",
                    content = @Content(schema = @Schema(implementation = ReviewFeedbackResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid decision status provided",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Feedback item with specified ID not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PatchMapping("/feedback/{id}/decision")
    public ResponseEntity<ReviewFeedbackResponse> updateFeedbackDecision(
            @Parameter(description = "ID of the review feedback item", required = true, example = "1")
            @PathVariable("id") Long id,
            @Valid @RequestBody FeedbackDecisionRequest request) {
        log.info("Updating decision for feedback id={} to status={}", id, request.status());
        ReviewFeedbackResponse response = reviewService.updateFeedbackDecision(id, request.status());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a complete review session by ID, including source code and all feedback items.
     */
    @Operation(
            summary = "Get review session by ID",
            description = "Retrieves full details of a review session including source code, Gemini execution metrics, and all feedback items."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review session details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ReviewSessionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Review session with specified ID not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/sessions/{id}")
    public ResponseEntity<ReviewSessionResponse> getSessionById(
            @Parameter(description = "ID of the review session", required = true, example = "1")
            @PathVariable("id") Long id) {
        ReviewSessionResponse response = reviewService.getSessionById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paginated history list of review sessions.
     */
    @Operation(
            summary = "List recent review sessions",
            description = "Returns a paginated list of previous review sessions with status and summary metrics."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of review sessions retrieved successfully")
    })
    @GetMapping("/sessions")
    public ResponseEntity<Page<ReviewSessionSummaryResponse>> listSessions(
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        Page<ReviewSessionSummaryResponse> page = reviewService.listRecentSessions(pageable);
        return ResponseEntity.ok(page);
    }
}
