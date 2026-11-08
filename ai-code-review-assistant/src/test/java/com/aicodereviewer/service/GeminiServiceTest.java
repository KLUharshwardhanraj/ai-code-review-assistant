package com.aicodereviewer.service;

import com.aicodereviewer.config.GeminiProperties;
import com.aicodereviewer.dto.gemini.FeedbackItemDto;
import com.aicodereviewer.dto.gemini.GeminiResponseDto;
import com.aicodereviewer.exception.GeminiApiException;
import com.aicodereviewer.exception.GeminiParsingException;
import com.aicodereviewer.metrics.ReviewMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeminiService Unit & Parsing Tests")
class GeminiServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private ReviewMetrics reviewMetrics;

    private ObjectMapper geminiObjectMapper;
    private GeminiProperties geminiProperties;
    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        geminiObjectMapper = new ObjectMapper();
        geminiProperties = new GeminiProperties();
        geminiProperties.setKey("test-api-key");
        geminiProperties.setModel("gemini-2.5-flash");
        geminiProperties.setUrl("https://generativelanguage.googleapis.com/v1beta/models");

        lenient().when(reviewMetrics.startTimer()).thenReturn(mock(Timer.Sample.class));

        geminiService = new GeminiService(
                restClient,
                geminiObjectMapper,
                geminiProperties,
                reviewMetrics
        );
    }

    private void mockRestClientChain(GeminiResponseDto responseDto) {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(GeminiResponseDto.class)).thenReturn(responseDto);
    }

    @Nested
    @DisplayName("Structured JSON Parsing")
    class ParsingTests {

        @Test
        @DisplayName("Should successfully parse Gemini structured JSON array into FeedbackItemDto list")
        void shouldParseStructuredJsonSuccessfully() {
            // Arrange
            String jsonPayload = """
                    [
                      {
                        "lineNumber": 15,
                        "category": "BUG",
                        "severity": "CRITICAL",
                        "suggestion": "Resource leak: FileInputStream is not closed.",
                        "replacementCode": "try (FileInputStream fis = new FileInputStream(file)) { ... }"
                      },
                      {
                        "lineNumber": 22,
                        "category": "READABILITY",
                        "severity": "INFO",
                        "suggestion": "Variable name 'a' is ambiguous; consider 'totalAmount'.",
                        "replacementCode": null
                      }
                    ]
                    """;

            GeminiResponseDto.Candidate candidate = new GeminiResponseDto.Candidate(
                    new GeminiResponseDto.Content(
                            List.of(new GeminiResponseDto.Part(jsonPayload)),
                            "model"
                    ),
                    "STOP"
            );

            GeminiResponseDto responseDto = new GeminiResponseDto(
                    List.of(candidate),
                    new GeminiResponseDto.UsageMetadata(100, 200, 300),
                    "gemini-2.5-flash"
            );

            mockRestClientChain(responseDto);

            // Act
            List<FeedbackItemDto> items = geminiService.reviewCode("int a = 10;", "java");

            // Assert
            assertThat(items).hasSize(2);

            FeedbackItemDto item1 = items.get(0);
            assertThat(item1.lineNumber()).isEqualTo(15);
            assertThat(item1.category()).isEqualTo("BUG");
            assertThat(item1.severity()).isEqualTo("CRITICAL");
            assertThat(item1.suggestion()).contains("FileInputStream is not closed");
            assertThat(item1.replacementCode()).contains("try (FileInputStream fis");

            FeedbackItemDto item2 = items.get(1);
            assertThat(item2.lineNumber()).isEqualTo(22);
            assertThat(item2.category()).isEqualTo("READABILITY");
            assertThat(item2.severity()).isEqualTo("INFO");
            assertThat(item2.replacementCode()).isNull();

            verify(reviewMetrics).recordGeminiLatency(any(), eq("gemini-2.5-flash"), eq("success"));
        }

        @Test
        @DisplayName("Should return empty list when Gemini returns empty JSON array")
        void shouldReturnEmptyList_WhenCodeIsClean() {
            // Arrange
            GeminiResponseDto.Candidate candidate = new GeminiResponseDto.Candidate(
                    new GeminiResponseDto.Content(
                            List.of(new GeminiResponseDto.Part("[]")),
                            "model"
                    ),
                    "STOP"
            );

            GeminiResponseDto responseDto = new GeminiResponseDto(
                    List.of(candidate),
                    new GeminiResponseDto.UsageMetadata(50, 10, 60),
                    "gemini-2.5-flash"
            );

            mockRestClientChain(responseDto);

            // Act
            List<FeedbackItemDto> items = geminiService.reviewCode("clean code", "java");

            // Assert
            assertThat(items).isEmpty();
            verify(reviewMetrics).recordGeminiLatency(any(), eq("gemini-2.5-flash"), eq("success"));
        }
    }

    @Nested
    @DisplayName("Defensive Error Handling")
    class DefensiveHandlingTests {

        @Test
        @DisplayName("Should throw GeminiApiException when candidates list is empty")
        void shouldThrowGeminiApiException_WhenCandidatesEmpty() {
            GeminiResponseDto emptyDto = new GeminiResponseDto(Collections.emptyList(), null, "gemini-2.5-flash");
            mockRestClientChain(emptyDto);

            assertThatThrownBy(() -> geminiService.reviewCode("code", "java"))
                    .isInstanceOf(GeminiApiException.class)
                    .hasMessageContaining("empty candidates list");

            verify(reviewMetrics).incrementGeminiFailure("api_error");
        }

        @Test
        @DisplayName("Should throw GeminiApiException when finishReason is not STOP (e.g. SAFETY)")
        void shouldThrowGeminiApiException_WhenFinishReasonIsSafety() {
            GeminiResponseDto.Candidate candidate = new GeminiResponseDto.Candidate(
                    new GeminiResponseDto.Content(List.of(new GeminiResponseDto.Part("text")), "model"),
                    "SAFETY"
            );
            GeminiResponseDto responseDto = new GeminiResponseDto(List.of(candidate), null, "gemini-2.5-flash");
            mockRestClientChain(responseDto);

            assertThatThrownBy(() -> geminiService.reviewCode("code", "java"))
                    .isInstanceOf(GeminiApiException.class)
                    .hasMessageContaining("finishReason='SAFETY'");

            verify(reviewMetrics).incrementGeminiFailure("api_error");
        }

        @Test
        @DisplayName("Should throw GeminiParsingException when returned text is malformed JSON")
        void shouldThrowGeminiParsingException_WhenJsonIsMalformed() {
            GeminiResponseDto.Candidate candidate = new GeminiResponseDto.Candidate(
                    new GeminiResponseDto.Content(List.of(new GeminiResponseDto.Part("INVALID NOT JSON")), "model"),
                    "STOP"
            );
            GeminiResponseDto responseDto = new GeminiResponseDto(List.of(candidate), null, "gemini-2.5-flash");
            mockRestClientChain(responseDto);

            assertThatThrownBy(() -> geminiService.reviewCode("code", "java"))
                    .isInstanceOf(GeminiParsingException.class)
                    .hasMessageContaining("Failed to parse Gemini feedback JSON");

            verify(reviewMetrics).incrementGeminiFailure("json_parse_error");
        }
    }

    @Nested
    @DisplayName("Resilience & Fallback Recovery")
    class RecoveryTests {

        @Test
        @DisplayName("Should execute @Recover fallback and return safe system item when retries exhaust")
        void shouldRecoverGracefully_WhenRetryExhausted() {
            // Arrange
            Exception exception = new RuntimeException("503 Service Unavailable");

            // Act
            List<FeedbackItemDto> fallbackItems = geminiService.recoverReviewCode(exception, "code", "python");

            // Assert
            assertThat(fallbackItems).hasSize(1);
            FeedbackItemDto fallback = fallbackItems.get(0);
            assertThat(fallback.category()).isEqualTo("SYSTEM");
            assertThat(fallback.severity()).isEqualTo("WARNING");
            assertThat(fallback.suggestion()).contains("AI code review service is currently unavailable");
            assertThat(fallback.lineNumber()).isNull();
            assertThat(fallback.replacementCode()).isNull();

            verify(reviewMetrics).incrementGeminiFailure("retry_exhausted_fallback");
        }
    }
}
