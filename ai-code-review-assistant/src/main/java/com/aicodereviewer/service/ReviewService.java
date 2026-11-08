package com.aicodereviewer.service;

import com.aicodereviewer.dto.request.FeedbackDecisionRequest;
import com.aicodereviewer.dto.request.SubmitCodeRequest;
import com.aicodereviewer.dto.response.ReviewFeedbackResponse;
import com.aicodereviewer.dto.response.ReviewSessionResponse;
import com.aicodereviewer.dto.response.ReviewSessionSummaryResponse;
import com.aicodereviewer.entity.enums.FeedbackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service contract orchestrating review sessions, Gemini AI integration,
 * and human-in-the-loop review decisions.
 */
public interface ReviewService {

    /**
     * Submits source code or a Git diff for automated Gemini code review,
     * stores the session and generated feedback items, and returns the full response.
     *
     * @param request code submission payload
     * @return the created review session with all feedback items
     */
    ReviewSessionResponse createReviewSession(SubmitCodeRequest request);

    /**
     * Updates the human decision (ACCEPT or REJECT) for a single feedback item.
     *
     * @param feedbackId ID of the feedback item to update
     * @param newStatus  the new human decision (ACCEPTED or REJECTED)
     * @return the updated feedback item response
     */
    ReviewFeedbackResponse updateFeedbackDecision(Long feedbackId, FeedbackStatus newStatus);

    /**
     * Retrieves a full review session by ID, including all feedback items.
     *
     * @param sessionId the session identifier
     * @return the complete review session details
     */
    ReviewSessionResponse getSessionById(Long sessionId);

    /**
     * Retrieves a paginated history of review sessions ordered by creation date descending.
     *
     * @param pageable pagination parameters
     * @return a page of review session summaries
     */
    Page<ReviewSessionSummaryResponse> listRecentSessions(Pageable pageable);
}
