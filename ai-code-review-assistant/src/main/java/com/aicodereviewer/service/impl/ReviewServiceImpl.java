package com.aicodereviewer.service.impl;

import com.aicodereviewer.config.GeminiProperties;
import com.aicodereviewer.dto.gemini.FeedbackItemDto;
import com.aicodereviewer.dto.request.SubmitCodeRequest;
import com.aicodereviewer.dto.response.ReviewFeedbackResponse;
import com.aicodereviewer.dto.response.ReviewSessionResponse;
import com.aicodereviewer.dto.response.ReviewSessionSummaryResponse;
import com.aicodereviewer.entity.ReviewFeedback;
import com.aicodereviewer.entity.ReviewSession;
import com.aicodereviewer.entity.enums.*;
import com.aicodereviewer.exception.InvalidDecisionException;
import com.aicodereviewer.exception.ResourceNotFoundException;
import com.aicodereviewer.repository.ReviewFeedbackRepository;
import com.aicodereviewer.repository.ReviewSessionRepository;
import com.aicodereviewer.service.GeminiService;
import com.aicodereviewer.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Production implementation of {@link ReviewService}.
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewSessionRepository sessionRepository;
    private final ReviewFeedbackRepository feedbackRepository;
    private final GeminiService geminiService;
    private final GeminiProperties geminiProperties;
    private final com.aicodereviewer.metrics.ReviewMetrics reviewMetrics;

    public ReviewServiceImpl(
            ReviewSessionRepository sessionRepository,
            ReviewFeedbackRepository feedbackRepository,
            GeminiService geminiService,
            GeminiProperties geminiProperties,
            com.aicodereviewer.metrics.ReviewMetrics reviewMetrics
    ) {
        this.sessionRepository = sessionRepository;
        this.feedbackRepository = feedbackRepository;
        this.geminiService = geminiService;
        this.geminiProperties = geminiProperties;
        this.reviewMetrics = reviewMetrics;
    }

    @Override
    @Transactional
    public ReviewSessionResponse createReviewSession(SubmitCodeRequest request) {
        String language = request.language();
        SubmissionType submissionType = request.submissionType() != null ? request.submissionType() : SubmissionType.SNIPPET;
        String title = resolveTitle(request.title(), language, submissionType);

        log.info("Creating review session: title='{}', language='{}', type='{}'", title, language, submissionType);

        // Record submission counter metric
        reviewMetrics.incrementSubmissions(language, submissionType.name());
        io.micrometer.core.instrument.Timer.Sample sessionSample = reviewMetrics.startTimer();

        // 1. Initialise and persist session in PENDING status
        ReviewSession session = ReviewSession.builder()
                .title(title)
                .sourceCode(request.code())
                .language(language)
                .submissionType(submissionType)
                .status(ReviewStatus.PENDING)
                .geminiModel(geminiProperties.getModel())
                .build();

        session = sessionRepository.save(session);

        // 2. Query Gemini API with execution time tracking
        long startTime = System.currentTimeMillis();
        List<FeedbackItemDto> feedbackItems;
        try {
            feedbackItems = geminiService.reviewCode(request.code(), language);
        } catch (Exception ex) {
            log.error("Failed to obtain review feedback from Gemini for session id={}", session.getId(), ex);
            reviewMetrics.recordSessionProcessingTime(sessionSample, language, "failure");
            session.setStatus(ReviewStatus.FAILED);
            session.setErrorMessage(ex.getMessage());
            sessionRepository.save(session);
            throw ex;
        }
        long durationMs = System.currentTimeMillis() - startTime;
        session.setGeminiRequestDurationMs(durationMs);

        // 3. Map returned FeedbackItemDto list into ReviewFeedback entities
        for (FeedbackItemDto item : feedbackItems) {
            ReviewFeedback feedback = ReviewFeedback.builder()
                    .category(parseCategory(item.category()))
                    .severity(parseSeverity(item.severity()))
                    .resolution(FeedbackResolution.PENDING)
                    .title(generateFeedbackTitle(item.suggestion()))
                    .description(item.suggestion())
                    .lineStart(item.lineNumber())
                    .lineEnd(item.lineNumber())
                    .suggestedCode(item.replacementCode())
                    .build();

            session.addFeedback(feedback);
        }

        // 4. Update session lifecycle status
        session.setStatus(ReviewStatus.IN_REVIEW);
        ReviewSession savedSession = sessionRepository.save(session);

        reviewMetrics.recordSessionProcessingTime(sessionSample, language, "success");

        log.info("Review session created successfully: id={}, feedbackCount={}, durationMs={}",
                savedSession.getId(), savedSession.getTotalFeedbackCount(), durationMs);

        return ReviewSessionResponse.fromEntity(savedSession);
    }

    @Override
    @Transactional
    public ReviewFeedbackResponse updateFeedbackDecision(Long feedbackId, FeedbackStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Decision status must not be null.");
        }

        ReviewFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewFeedback", feedbackId));

        FeedbackResolution targetResolution = FeedbackResolution.valueOf(newStatus.name());
        FeedbackResolution currentResolution = feedback.getResolution();

        // Idempotent update: if already in the target state, return as is
        if (currentResolution == targetResolution) {
            log.debug("Feedback id={} already has resolution={}, skipping update", feedbackId, targetResolution);
            return ReviewFeedbackResponse.fromEntity(feedback);
        }

        log.info("Updating feedback decision: id={}, from={}, to={}", feedbackId, currentResolution, targetResolution);

        feedback.setResolution(targetResolution);
        ReviewFeedback updatedFeedback = feedbackRepository.save(feedback);

        // Record decision counter metric (accepted/rejected) tagged by category
        String category = feedback.getCategory() != null ? feedback.getCategory().name() : "OTHER";
        reviewMetrics.incrementFeedbackDecision(newStatus.name(), category);

        // Update denormalized counters on the parent session
        ReviewSession session = feedback.getSession();
        if (session != null) {
            long accepted = feedbackRepository.countBySessionIdAndResolution(session.getId(), FeedbackResolution.ACCEPTED);
            long rejected = feedbackRepository.countBySessionIdAndResolution(session.getId(), FeedbackResolution.REJECTED);
            long pending = feedbackRepository.countBySessionIdAndResolution(session.getId(), FeedbackResolution.PENDING);

            session.setAcceptedCount((int) accepted);
            session.setRejectedCount((int) rejected);

            // If all feedback items have been resolved, transition session to COMPLETED
            if (pending == 0 && session.getTotalFeedbackCount() > 0) {
                session.setStatus(ReviewStatus.COMPLETED);
                log.info("All feedback items resolved for session id={}, transitioning to COMPLETED", session.getId());
            } else if (session.getStatus() == ReviewStatus.COMPLETED && pending > 0) {
                session.setStatus(ReviewStatus.IN_REVIEW);
            }

            sessionRepository.save(session);
        }

        return ReviewFeedbackResponse.fromEntity(updatedFeedback);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewSessionResponse getSessionById(Long sessionId) {
        log.debug("Fetching review session by id={}", sessionId);
        ReviewSession session = sessionRepository.findByIdWithFeedbacks(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewSession", sessionId));
        return ReviewSessionResponse.fromEntity(session);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewSessionSummaryResponse> listRecentSessions(Pageable pageable) {
        log.debug("Listing recent review sessions with pageable={}", pageable);
        return sessionRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(ReviewSessionSummaryResponse::fromEntity);
    }

    // ─── Private Helper Methods ───

    private String resolveTitle(String customTitle, String language, SubmissionType type) {
        if (customTitle != null && !customTitle.trim().isEmpty()) {
            return customTitle.trim();
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return String.format("%s %s Review (%s)",
                language.substring(0, 1).toUpperCase() + language.substring(1),
                type == SubmissionType.DIFF ? "Diff" : "Snippet",
                timestamp);
    }

    private String generateFeedbackTitle(String suggestion) {
        if (suggestion == null || suggestion.isBlank()) {
            return "Review Feedback";
        }
        String firstSentence = suggestion.split("\\.\\s|\\n")[0].trim();
        if (firstSentence.length() > 80) {
            return firstSentence.substring(0, 77) + "...";
        }
        return firstSentence;
    }

    private FeedbackCategory parseCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.isBlank()) {
            return FeedbackCategory.OTHER;
        }
        try {
            return FeedbackCategory.valueOf(categoryStr.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return FeedbackCategory.OTHER;
        }
    }

    private FeedbackSeverity parseSeverity(String severityStr) {
        if (severityStr == null || severityStr.isBlank()) {
            return FeedbackSeverity.INFO;
        }
        try {
            return FeedbackSeverity.valueOf(severityStr.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return FeedbackSeverity.INFO;
        }
    }
}
