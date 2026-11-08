package com.aicodereviewer.service;

import com.aicodereviewer.config.GeminiProperties;
import com.aicodereviewer.dto.gemini.FeedbackItemDto;
import com.aicodereviewer.dto.request.SubmitCodeRequest;
import com.aicodereviewer.dto.response.ReviewFeedbackResponse;
import com.aicodereviewer.dto.response.ReviewSessionResponse;
import com.aicodereviewer.entity.ReviewFeedback;
import com.aicodereviewer.entity.ReviewSession;
import com.aicodereviewer.entity.enums.*;
import com.aicodereviewer.exception.ResourceNotFoundException;
import com.aicodereviewer.metrics.ReviewMetrics;
import com.aicodereviewer.repository.ReviewFeedbackRepository;
import com.aicodereviewer.repository.ReviewSessionRepository;
import com.aicodereviewer.service.impl.ReviewServiceImpl;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Unit Tests")
class ReviewServiceTest {

    @Mock
    private ReviewSessionRepository sessionRepository;

    @Mock
    private ReviewFeedbackRepository feedbackRepository;

    @Mock
    private GeminiService geminiService;

    @Mock
    private GeminiProperties geminiProperties;

    @Mock
    private ReviewMetrics reviewMetrics;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        lenient().when(geminiProperties.getModel()).thenReturn("gemini-2.5-flash");
        lenient().when(reviewMetrics.startTimer()).thenReturn(mock(Timer.Sample.class));
    }

    @Nested
    @DisplayName("createReviewSession")
    class CreateReviewSessionTests {

        @Test
        @DisplayName("Should create review session when Gemini returns feedback items")
        void shouldCreateReviewSession_WhenGeminiReturnsFeedback() {
            // Arrange
            String code = "public class Example { void run() { String s = null; s.trim(); } }";
            SubmitCodeRequest request = new SubmitCodeRequest(code, "java", SubmissionType.SNIPPET, "Null Pointer Test");

            FeedbackItemDto itemDto = new FeedbackItemDto(
                    2,
                    "BUG",
                    "CRITICAL",
                    "Potential NullPointerException on s.trim().",
                    "if (s != null) { s.trim(); }"
            );

            when(geminiService.reviewCode(code, "java")).thenReturn(List.of(itemDto));

            when(sessionRepository.save(any(ReviewSession.class))).thenAnswer(invocation -> {
                ReviewSession session = invocation.getArgument(0);
                if (session.getId() == null) {
                    session.setId(1L);
                }
                return session;
            });

            // Act
            ReviewSessionResponse response = reviewService.createReviewSession(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.sessionId()).isEqualTo(1L);
            assertThat(response.title()).isEqualTo("Null Pointer Test");
            assertThat(response.language()).isEqualTo("java");
            assertThat(response.status()).isEqualTo(ReviewStatus.IN_REVIEW);
            assertThat(response.totalCount()).isEqualTo(1);
            assertThat(response.feedbacks()).hasSize(1);

            ReviewFeedbackResponse feedback = response.feedbacks().get(0);
            assertThat(feedback.lineNumber()).isEqualTo(2);
            assertThat(feedback.category()).isEqualTo("BUG");
            assertThat(feedback.severity()).isEqualTo("CRITICAL");
            assertThat(feedback.replacementCode()).isEqualTo("if (s != null) { s.trim(); }");
            assertThat(feedback.status()).isEqualTo("PENDING");

            verify(reviewMetrics).incrementSubmissions("java", "SNIPPET");
            verify(reviewMetrics).recordSessionProcessingTime(any(), eq("java"), eq("success"));
            verify(sessionRepository, times(2)).save(any(ReviewSession.class));
        }

        @Test
        @DisplayName("Should handle system fallback gracefully when Gemini returns fallback item")
        void shouldHandleSystemFallback_WhenGeminiReturnsFallbackItem() {
            // Arrange
            SubmitCodeRequest request = new SubmitCodeRequest("const x = 1;", "javascript", SubmissionType.SNIPPET, null);

            FeedbackItemDto fallbackItem = new FeedbackItemDto(
                    null,
                    "SYSTEM",
                    "WARNING",
                    "AI code review service is currently unavailable or rate-limited. Please retry shortly.",
                    null
            );

            when(geminiService.reviewCode(anyString(), anyString())).thenReturn(List.of(fallbackItem));
            when(sessionRepository.save(any(ReviewSession.class))).thenAnswer(invocation -> {
                ReviewSession s = invocation.getArgument(0);
                if (s.getId() == null) s.setId(10L);
                return s;
            });

            // Act
            ReviewSessionResponse response = reviewService.createReviewSession(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.feedbacks()).hasSize(1);
            ReviewFeedbackResponse fb = response.feedbacks().get(0);
            assertThat(fb.category()).isEqualTo("SYSTEM");
            assertThat(fb.severity()).isEqualTo("WARNING");
            assertThat(fb.lineNumber()).isNull();
        }

        @Test
        @DisplayName("Should mark session FAILED and rethrow if Gemini throws unhandled exception")
        void shouldMarkSessionFailed_WhenGeminiFails() {
            // Arrange
            SubmitCodeRequest request = new SubmitCodeRequest("print('hi')", "python", SubmissionType.SNIPPET, null);
            when(sessionRepository.save(any(ReviewSession.class))).thenAnswer(i -> {
                ReviewSession s = i.getArgument(0);
                s.setId(5L);
                return s;
            });
            when(geminiService.reviewCode(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Connection refused"));

            // Act & Assert
            assertThatThrownBy(() -> reviewService.createReviewSession(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Connection refused");

            verify(reviewMetrics).recordSessionProcessingTime(any(), eq("python"), eq("failure"));
        }
    }

    @Nested
    @DisplayName("updateFeedbackDecision")
    class UpdateFeedbackDecisionTests {

        @Test
        @DisplayName("Should update feedback decision from PENDING to ACCEPTED and update session counters")
        void shouldUpdateFeedbackDecision_FromPendingToAccepted() {
            // Arrange
            ReviewSession session = ReviewSession.builder()
                    .id(1L)
                    .title("Test Session")
                    .sourceCode("code")
                    .language("java")
                    .totalFeedbackCount(2)
                    .acceptedCount(0)
                    .rejectedCount(0)
                    .status(ReviewStatus.IN_REVIEW)
                    .build();

            ReviewFeedback feedback = ReviewFeedback.builder()
                    .id(100L)
                    .session(session)
                    .category(FeedbackCategory.BUG)
                    .severity(FeedbackSeverity.HIGH)
                    .resolution(FeedbackResolution.PENDING)
                    .title("Bug title")
                    .description("Fix bug")
                    .build();

            when(feedbackRepository.findById(100L)).thenReturn(Optional.of(feedback));
            when(feedbackRepository.save(any(ReviewFeedback.class))).thenAnswer(i -> i.getArgument(0));

            // Mock session counter queries
            when(feedbackRepository.countBySessionIdAndResolution(1L, FeedbackResolution.ACCEPTED)).thenReturn(1L);
            when(feedbackRepository.countBySessionIdAndResolution(1L, FeedbackResolution.REJECTED)).thenReturn(0L);
            when(feedbackRepository.countBySessionIdAndResolution(1L, FeedbackResolution.PENDING)).thenReturn(1L);

            // Act
            ReviewFeedbackResponse response = reviewService.updateFeedbackDecision(100L, FeedbackStatus.ACCEPTED);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo("ACCEPTED");
            assertThat(feedback.getResolution()).isEqualTo(FeedbackResolution.ACCEPTED);

            verify(reviewMetrics).incrementFeedbackDecision("ACCEPTED", "BUG");
            verify(feedbackRepository).save(feedback);
            verify(sessionRepository).save(session);
            assertThat(session.getAcceptedCount()).isEqualTo(1);
            assertThat(session.getStatus()).isEqualTo(ReviewStatus.IN_REVIEW);
        }

        @Test
        @DisplayName("Should transition session status to COMPLETED when all feedback items are resolved")
        void shouldTransitionSessionToCompleted_WhenAllFeedbacksResolved() {
            // Arrange
            ReviewSession session = ReviewSession.builder()
                    .id(1L)
                    .totalFeedbackCount(1)
                    .acceptedCount(0)
                    .rejectedCount(0)
                    .status(ReviewStatus.IN_REVIEW)
                    .build();

            ReviewFeedback feedback = ReviewFeedback.builder()
                    .id(101L)
                    .session(session)
                    .category(FeedbackCategory.SECURITY)
                    .severity(FeedbackSeverity.CRITICAL)
                    .resolution(FeedbackResolution.PENDING)
                    .title("SQL Injection")
                    .description("Use PreparedStatement")
                    .build();

            when(feedbackRepository.findById(101L)).thenReturn(Optional.of(feedback));
            when(feedbackRepository.save(any(ReviewFeedback.class))).thenAnswer(i -> i.getArgument(0));

            // All resolved: 1 accepted, 0 pending
            when(feedbackRepository.countBySessionIdAndResolution(1L, FeedbackResolution.ACCEPTED)).thenReturn(1L);
            when(feedbackRepository.countBySessionIdAndResolution(1L, FeedbackResolution.REJECTED)).thenReturn(0L);
            when(feedbackRepository.countBySessionIdAndResolution(1L, FeedbackResolution.PENDING)).thenReturn(0L);

            // Act
            ReviewFeedbackResponse response = reviewService.updateFeedbackDecision(101L, FeedbackStatus.ACCEPTED);

            // Assert
            assertThat(response.status()).isEqualTo("ACCEPTED");
            assertThat(session.getStatus()).isEqualTo(ReviewStatus.COMPLETED);
            verify(sessionRepository).save(session);
        }

        @Test
        @DisplayName("Should update feedback decision from PENDING to REJECTED")
        void shouldUpdateFeedbackDecision_FromPendingToRejected() {
            // Arrange
            ReviewSession session = ReviewSession.builder().id(2L).totalFeedbackCount(1).build();
            ReviewFeedback feedback = ReviewFeedback.builder()
                    .id(200L)
                    .session(session)
                    .category(FeedbackCategory.READABILITY)
                    .severity(FeedbackSeverity.LOW)
                    .resolution(FeedbackResolution.PENDING)
                    .title("Rename variable")
                    .description("Consider renaming x to count")
                    .build();

            when(feedbackRepository.findById(200L)).thenReturn(Optional.of(feedback));
            when(feedbackRepository.save(any(ReviewFeedback.class))).thenAnswer(i -> i.getArgument(0));

            when(feedbackRepository.countBySessionIdAndResolution(2L, FeedbackResolution.ACCEPTED)).thenReturn(0L);
            when(feedbackRepository.countBySessionIdAndResolution(2L, FeedbackResolution.REJECTED)).thenReturn(1L);
            when(feedbackRepository.countBySessionIdAndResolution(2L, FeedbackResolution.PENDING)).thenReturn(0L);

            // Act
            ReviewFeedbackResponse response = reviewService.updateFeedbackDecision(200L, FeedbackStatus.REJECTED);

            // Assert
            assertThat(response.status()).isEqualTo("REJECTED");
            verify(reviewMetrics).incrementFeedbackDecision("REJECTED", "READABILITY");
        }

        @Test
        @DisplayName("Should be idempotent when updating feedback with the same resolution")
        void shouldBeIdempotent_WhenTargetStatusMatchesCurrent() {
            // Arrange
            ReviewFeedback feedback = ReviewFeedback.builder()
                    .id(300L)
                    .resolution(FeedbackResolution.ACCEPTED)
                    .category(FeedbackCategory.BUG)
                    .title("Bug")
                    .description("Desc")
                    .build();

            when(feedbackRepository.findById(300L)).thenReturn(Optional.of(feedback));

            // Act
            ReviewFeedbackResponse response = reviewService.updateFeedbackDecision(300L, FeedbackStatus.ACCEPTED);

            // Assert
            assertThat(response.status()).isEqualTo("ACCEPTED");
            verify(feedbackRepository, never()).save(any());
            verify(reviewMetrics, never()).incrementFeedbackDecision(any(), any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when status is null")
        void shouldThrowException_WhenStatusIsNull() {
            assertThatThrownBy(() -> reviewService.updateFeedbackDecision(1L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Decision status must not be null");
        }
    }

    @Nested
    @DisplayName("Resource Lookup & Error Handling")
    class LookupTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when session ID is missing")
        void shouldThrowResourceNotFoundException_WhenSessionNotFound() {
            when(sessionRepository.findByIdWithFeedbacks(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.getSessionById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ReviewSession not found with id: 999");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when feedback ID is missing")
        void shouldThrowResourceNotFoundException_WhenFeedbackNotFound() {
            when(feedbackRepository.findById(888L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.updateFeedbackDecision(888L, FeedbackStatus.ACCEPTED))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ReviewFeedback not found with id: 888");
        }
    }
}
