package com.aicodereviewer.entity;

import com.aicodereviewer.entity.enums.ReviewStatus;
import com.aicodereviewer.entity.enums.SubmissionType;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single code review session.
 */
@Entity
@Table(name = "review_sessions", indexes = {
        @Index(name = "idx_session_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class ReviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "source_code", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String sourceCode;

    @Column(nullable = false, length = 50)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", nullable = false, length = 20)
    private SubmissionType submissionType = SubmissionType.SNIPPET;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "total_feedback_count", nullable = false)
    private Integer totalFeedbackCount = 0;

    @Column(name = "accepted_count", nullable = false)
    private Integer acceptedCount = 0;

    @Column(name = "rejected_count", nullable = false)
    private Integer rejectedCount = 0;

    @Column(name = "gemini_model", length = 100)
    private String geminiModel;

    @Column(name = "gemini_request_duration_ms")
    private Long geminiRequestDurationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReviewFeedback> feedbackItems = new ArrayList<>();

    public ReviewSession() {}

    public ReviewSession(Long id, String title, String sourceCode, String language,
                         SubmissionType submissionType, ReviewStatus status,
                         Integer totalFeedbackCount, Integer acceptedCount, Integer rejectedCount,
                         String geminiModel, Long geminiRequestDurationMs, String errorMessage,
                         LocalDateTime createdAt, LocalDateTime updatedAt,
                         List<ReviewFeedback> feedbackItems) {
        this.id = id;
        this.title = title;
        this.sourceCode = sourceCode;
        this.language = language;
        this.submissionType = submissionType != null ? submissionType : SubmissionType.SNIPPET;
        this.status = status != null ? status : ReviewStatus.PENDING;
        this.totalFeedbackCount = totalFeedbackCount != null ? totalFeedbackCount : 0;
        this.acceptedCount = acceptedCount != null ? acceptedCount : 0;
        this.rejectedCount = rejectedCount != null ? rejectedCount : 0;
        this.geminiModel = geminiModel;
        this.geminiRequestDurationMs = geminiRequestDurationMs;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.feedbackItems = feedbackItems != null ? feedbackItems : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public void addFeedback(ReviewFeedback feedback) {
        feedbackItems.add(feedback);
        feedback.setSession(this);
        this.totalFeedbackCount = feedbackItems.size();
    }

    public void removeFeedback(ReviewFeedback feedback) {
        feedbackItems.remove(feedback);
        feedback.setSession(null);
        this.totalFeedbackCount = feedbackItems.size();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public SubmissionType getSubmissionType() { return submissionType; }
    public void setSubmissionType(SubmissionType submissionType) { this.submissionType = submissionType; }

    public ReviewStatus getStatus() { return status; }
    public void setStatus(ReviewStatus status) { this.status = status; }

    public Integer getTotalFeedbackCount() { return totalFeedbackCount; }
    public void setTotalFeedbackCount(Integer totalFeedbackCount) { this.totalFeedbackCount = totalFeedbackCount; }

    public Integer getAcceptedCount() { return acceptedCount; }
    public void setAcceptedCount(Integer acceptedCount) { this.acceptedCount = acceptedCount; }

    public Integer getRejectedCount() { return rejectedCount; }
    public void setRejectedCount(Integer rejectedCount) { this.rejectedCount = rejectedCount; }

    public String getGeminiModel() { return geminiModel; }
    public void setGeminiModel(String geminiModel) { this.geminiModel = geminiModel; }

    public Long getGeminiRequestDurationMs() { return geminiRequestDurationMs; }
    public void setGeminiRequestDurationMs(Long geminiRequestDurationMs) { this.geminiRequestDurationMs = geminiRequestDurationMs; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<ReviewFeedback> getFeedbackItems() { return feedbackItems; }
    public void setFeedbackItems(List<ReviewFeedback> feedbackItems) { this.feedbackItems = feedbackItems; }

    public static class Builder {
        private Long id;
        private String title;
        private String sourceCode;
        private String language;
        private SubmissionType submissionType = SubmissionType.SNIPPET;
        private ReviewStatus status = ReviewStatus.PENDING;
        private Integer totalFeedbackCount = 0;
        private Integer acceptedCount = 0;
        private Integer rejectedCount = 0;
        private String geminiModel;
        private Long geminiRequestDurationMs;
        private String errorMessage;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<ReviewFeedback> feedbackItems = new ArrayList<>();

        public Builder id(Long id) { this.id = id; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder sourceCode(String sourceCode) { this.sourceCode = sourceCode; return this; }
        public Builder language(String language) { this.language = language; return this; }
        public Builder submissionType(SubmissionType submissionType) { this.submissionType = submissionType; return this; }
        public Builder status(ReviewStatus status) { this.status = status; return this; }
        public Builder totalFeedbackCount(Integer totalFeedbackCount) { this.totalFeedbackCount = totalFeedbackCount; return this; }
        public Builder acceptedCount(Integer acceptedCount) { this.acceptedCount = acceptedCount; return this; }
        public Builder rejectedCount(Integer rejectedCount) { this.rejectedCount = rejectedCount; return this; }
        public Builder geminiModel(String geminiModel) { this.geminiModel = geminiModel; return this; }
        public Builder geminiRequestDurationMs(Long geminiRequestDurationMs) { this.geminiRequestDurationMs = geminiRequestDurationMs; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder feedbackItems(List<ReviewFeedback> feedbackItems) { this.feedbackItems = feedbackItems; return this; }

        public ReviewSession build() {
            return new ReviewSession(id, title, sourceCode, language, submissionType, status,
                    totalFeedbackCount, acceptedCount, rejectedCount, geminiModel,
                    geminiRequestDurationMs, errorMessage, createdAt, updatedAt, feedbackItems);
        }
    }
}
