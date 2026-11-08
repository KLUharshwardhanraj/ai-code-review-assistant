package com.aicodereviewer.entity;

import com.aicodereviewer.entity.enums.FeedbackCategory;
import com.aicodereviewer.entity.enums.FeedbackResolution;
import com.aicodereviewer.entity.enums.FeedbackSeverity;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Represents a single feedback item within a {@link ReviewSession}.
 */
@Entity
@Table(name = "review_feedback", indexes = {
        @Index(name = "idx_feedback_session_id", columnList = "session_id"),
        @Index(name = "idx_feedback_resolution", columnList = "resolution")
})
@EntityListeners(AuditingEntityListener.class)
public class ReviewFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ReviewSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackResolution resolution = FeedbackResolution.PENDING;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "line_start")
    private Integer lineStart;

    @Column(name = "line_end")
    private Integer lineEnd;

    @Column(name = "original_code", columnDefinition = "TEXT")
    private String originalCode;

    @Column(name = "suggested_code", columnDefinition = "TEXT")
    private String suggestedCode;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ReviewFeedback() {}

    public ReviewFeedback(Long id, ReviewSession session, FeedbackCategory category, FeedbackSeverity severity,
                          FeedbackResolution resolution, String title, String description, Integer lineStart,
                          Integer lineEnd, String originalCode, String suggestedCode, LocalDateTime createdAt,
                          LocalDateTime updatedAt) {
        this.id = id;
        this.session = session;
        this.category = category;
        this.severity = severity;
        this.resolution = resolution != null ? resolution : FeedbackResolution.PENDING;
        this.title = title;
        this.description = description;
        this.lineStart = lineStart;
        this.lineEnd = lineEnd;
        this.originalCode = originalCode;
        this.suggestedCode = suggestedCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ReviewSession getSession() { return session; }
    public void setSession(ReviewSession session) { this.session = session; }

    public FeedbackCategory getCategory() { return category; }
    public void setCategory(FeedbackCategory category) { this.category = category; }

    public FeedbackSeverity getSeverity() { return severity; }
    public void setSeverity(FeedbackSeverity severity) { this.severity = severity; }

    public FeedbackResolution getResolution() { return resolution; }
    public void setResolution(FeedbackResolution resolution) { this.resolution = resolution; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getLineStart() { return lineStart; }
    public void setLineStart(Integer lineStart) { this.lineStart = lineStart; }

    public Integer getLineEnd() { return lineEnd; }
    public void setLineEnd(Integer lineEnd) { this.lineEnd = lineEnd; }

    public String getOriginalCode() { return originalCode; }
    public void setOriginalCode(String originalCode) { this.originalCode = originalCode; }

    public String getSuggestedCode() { return suggestedCode; }
    public void setSuggestedCode(String suggestedCode) { this.suggestedCode = suggestedCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static class Builder {
        private Long id;
        private ReviewSession session;
        private FeedbackCategory category;
        private FeedbackSeverity severity;
        private FeedbackResolution resolution = FeedbackResolution.PENDING;
        private String title;
        private String description;
        private Integer lineStart;
        private Integer lineEnd;
        private String originalCode;
        private String suggestedCode;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder session(ReviewSession session) { this.session = session; return this; }
        public Builder category(FeedbackCategory category) { this.category = category; return this; }
        public Builder severity(FeedbackSeverity severity) { this.severity = severity; return this; }
        public Builder resolution(FeedbackResolution resolution) { this.resolution = resolution; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder lineStart(Integer lineStart) { this.lineStart = lineStart; return this; }
        public Builder lineEnd(Integer lineEnd) { this.lineEnd = lineEnd; return this; }
        public Builder originalCode(String originalCode) { this.originalCode = originalCode; return this; }
        public Builder suggestedCode(String suggestedCode) { this.suggestedCode = suggestedCode; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ReviewFeedback build() {
            return new ReviewFeedback(id, session, category, severity, resolution, title, description,
                    lineStart, lineEnd, originalCode, suggestedCode, createdAt, updatedAt);
        }
    }
}
