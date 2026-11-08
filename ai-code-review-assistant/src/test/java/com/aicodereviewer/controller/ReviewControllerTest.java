package com.aicodereviewer.controller;

import com.aicodereviewer.dto.request.FeedbackDecisionRequest;
import com.aicodereviewer.dto.request.SubmitCodeRequest;
import com.aicodereviewer.dto.response.ReviewFeedbackResponse;
import com.aicodereviewer.dto.response.ReviewSessionResponse;
import com.aicodereviewer.dto.response.ReviewSessionSummaryResponse;
import com.aicodereviewer.entity.enums.FeedbackStatus;
import com.aicodereviewer.entity.enums.ReviewStatus;
import com.aicodereviewer.entity.enums.SubmissionType;
import com.aicodereviewer.exception.GlobalExceptionHandler;
import com.aicodereviewer.exception.ResourceNotFoundException;
import com.aicodereviewer.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("ReviewController Web Layer Slice Tests")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @Nested
    @DisplayName("POST /api/v1/reviews/submit")
    class SubmitCodeTests {

        @Test
        @DisplayName("Should return 201 Created when submission is valid")
        void shouldReturn201_WhenSubmissionIsValid() throws Exception {
            // Arrange
            SubmitCodeRequest request = new SubmitCodeRequest(
                    "public class Main { public static void main(String[] args) {} }",
                    "java",
                    SubmissionType.SNIPPET,
                    "Main Class Review"
            );

            ReviewFeedbackResponse feedback = new ReviewFeedbackResponse(
                    1L, 1, "READABILITY", "INFO", "Add javadoc comments.", null, "PENDING", LocalDateTime.now().toString()
            );

            ReviewSessionResponse expectedResponse = new ReviewSessionResponse(
                    10L,
                    "Main Class Review",
                    "java",
                    SubmissionType.SNIPPET,
                    request.code(),
                    ReviewStatus.IN_REVIEW,
                    "gemini-2.5-flash",
                    1200L,
                    LocalDateTime.now().toString(),
                    List.of(feedback),
                    1,
                    0,
                    0,
                    1
            );

            given(reviewService.createReviewSession(any(SubmitCodeRequest.class)))
                    .willReturn(expectedResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/reviews/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.session_id").value(10))
                    .andExpect(jsonPath("$.title").value("Main Class Review"))
                    .andExpect(jsonPath("$.language").value("java"))
                    .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                    .andExpect(jsonPath("$.total_count").value(1))
                    .andExpect(jsonPath("$.feedbacks", hasSize(1)))
                    .andExpect(jsonPath("$.feedbacks[0].id").value(1))
                    .andExpect(jsonPath("$.feedbacks[0].category").value("READABILITY"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request with field errors when code is blank")
        void shouldReturn400_WhenCodeIsBlank() throws Exception {
            SubmitCodeRequest invalidRequest = new SubmitCodeRequest(
                    "",
                    "java",
                    SubmissionType.SNIPPET,
                    "Blank Code Test"
            );

            mockMvc.perform(post("/api/v1/reviews/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.field_errors.code").exists());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when language is blank")
        void shouldReturn400_WhenLanguageIsBlank() throws Exception {
            SubmitCodeRequest invalidRequest = new SubmitCodeRequest(
                    "int x = 10;",
                    "",
                    SubmissionType.SNIPPET,
                    null
            );

            mockMvc.perform(post("/api/v1/reviews/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.field_errors.language").exists());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when code exceeds 50,000 characters")
        void shouldReturn400_WhenCodeExceedsMaxLimit() throws Exception {
            String oversizedCode = "a".repeat(50001);
            SubmitCodeRequest invalidRequest = new SubmitCodeRequest(
                    oversizedCode,
                    "java",
                    SubmissionType.SNIPPET,
                    null
            );

            mockMvc.perform(post("/api/v1/reviews/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.field_errors.code").value(containsString("must not exceed 50,000 characters")));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/reviews/feedback/{id}/decision")
    class FeedbackDecisionTests {

        @Test
        @DisplayName("Should return 200 OK when accepting a feedback item")
        void shouldReturn200_WhenAcceptingFeedback() throws Exception {
            FeedbackDecisionRequest request = new FeedbackDecisionRequest(FeedbackStatus.ACCEPTED);

            ReviewFeedbackResponse expectedResponse = new ReviewFeedbackResponse(
                    5L, 12, "BUG", "CRITICAL", "Fix NPE", "if (x != null)", "ACCEPTED", LocalDateTime.now().toString()
            );

            given(reviewService.updateFeedbackDecision(eq(5L), eq(FeedbackStatus.ACCEPTED)))
                    .willReturn(expectedResponse);

            mockMvc.perform(patch("/api/v1/reviews/feedback/{id}/decision", 5L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.status").value("ACCEPTED"))
                    .andExpect(jsonPath("$.category").value("BUG"));
        }

        @Test
        @DisplayName("Should return 200 OK when rejecting a feedback item")
        void shouldReturn200_WhenRejectingFeedback() throws Exception {
            FeedbackDecisionRequest request = new FeedbackDecisionRequest(FeedbackStatus.REJECTED);

            ReviewFeedbackResponse expectedResponse = new ReviewFeedbackResponse(
                    6L, 20, "READABILITY", "INFO", "Rename", null, "REJECTED", LocalDateTime.now().toString()
            );

            given(reviewService.updateFeedbackDecision(eq(6L), eq(FeedbackStatus.REJECTED)))
                    .willReturn(expectedResponse);

            mockMvc.perform(patch("/api/v1/reviews/feedback/{id}/decision", 6L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(6))
                    .andExpect(jsonPath("$.status").value("REJECTED"));
        }

        @Test
        @DisplayName("Should return 404 Not Found when feedback item does not exist")
        void shouldReturn404_WhenFeedbackNotFound() throws Exception {
            FeedbackDecisionRequest request = new FeedbackDecisionRequest(FeedbackStatus.ACCEPTED);

            given(reviewService.updateFeedbackDecision(eq(999L), any()))
                    .willThrow(new ResourceNotFoundException("ReviewFeedback", 999L));

            mockMvc.perform(patch("/api/v1/reviews/feedback/{id}/decision", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("ReviewFeedback not found with id: 999"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reviews/sessions/{id}")
    class GetSessionTests {

        @Test
        @DisplayName("Should return 200 OK with session details when session exists")
        void shouldReturn200_WhenSessionExists() throws Exception {
            ReviewSessionResponse sessionResponse = new ReviewSessionResponse(
                    1L,
                    "Order Review",
                    "java",
                    SubmissionType.SNIPPET,
                    "class Order {}",
                    ReviewStatus.COMPLETED,
                    "gemini-2.5-flash",
                    1500L,
                    LocalDateTime.now().toString(),
                    Collections.emptyList(),
                    0,
                    0,
                    0,
                    0
            );

            given(reviewService.getSessionById(1L)).willReturn(sessionResponse);

            mockMvc.perform(get("/api/v1/reviews/sessions/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.session_id").value(1))
                    .andExpect(jsonPath("$.title").value("Order Review"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("Should return 404 Not Found when session does not exist")
        void shouldReturn404_WhenSessionDoesNotExist() throws Exception {
            given(reviewService.getSessionById(404L))
                    .willThrow(new ResourceNotFoundException("ReviewSession", 404L));

            mockMvc.perform(get("/api/v1/reviews/sessions/{id}", 404L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("ReviewSession not found with id: 404"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reviews/sessions")
    class ListSessionsTests {

        @Test
        @DisplayName("Should return 200 OK with paginated list of sessions")
        void shouldReturn200_WithPaginatedSessions() throws Exception {
            ReviewSessionSummaryResponse summary = new ReviewSessionSummaryResponse(
                    1L,
                    "Test Title",
                    "java",
                    SubmissionType.SNIPPET,
                    ReviewStatus.IN_REVIEW,
                    3,
                    1,
                    1,
                    1,
                    "gemini-2.5-flash",
                    1400L,
                    LocalDateTime.now().toString()
            );

            Page<ReviewSessionSummaryResponse> page = new PageImpl<>(List.of(summary));
            given(reviewService.listRecentSessions(any(Pageable.class))).willReturn(page);

            mockMvc.perform(get("/api/v1/reviews/sessions")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].session_id").value(1))
                    .andExpect(jsonPath("$.content[0].total_feedback_count").value(3))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }
}
