package com.aicodereviewer.service;

import com.aicodereviewer.config.GeminiProperties;
import com.aicodereviewer.config.RetryConfig;
import com.aicodereviewer.dto.gemini.FeedbackItemDto;
import com.aicodereviewer.dto.gemini.GeminiResponseDto;
import com.aicodereviewer.metrics.ReviewMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Focused integration test proving Spring Retry AOP behavior for {@link GeminiService}:
 * <ul>
 *   <li>{@code @Retryable} intercepts transient errors (5xx, 429, socket timeouts) across 3 attempts.</li>
 *   <li>{@code @Recover} fallback executes when all retry attempts are exhausted, returning the safe system item.</li>
 * </ul>
 */
@SpringBootTest(classes = GeminiServiceRetryIntegrationTest.TestConfig.class)
@DisplayName("GeminiService Spring Retry & Resilience Integration Tests")
class GeminiServiceRetryIntegrationTest {

    @Configuration
    @Import(RetryConfig.class)
    static class TestConfig {

        @Bean
        public GeminiProperties geminiProperties() {
            GeminiProperties properties = new GeminiProperties();
            properties.setKey("test-key");
            properties.setModel("gemini-2.5-flash");
            properties.setUrl("https://generativelanguage.googleapis.com/v1beta/models");
            return properties;
        }

        @Bean(name = "geminiObjectMapper")
        public ObjectMapper geminiObjectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public GeminiService geminiService(
                RestClient restClient,
                ObjectMapper geminiObjectMapper,
                GeminiProperties geminiProperties,
                ReviewMetrics reviewMetrics
        ) {
            return new GeminiService(restClient, geminiObjectMapper, geminiProperties, reviewMetrics);
        }
    }

    @Autowired
    private GeminiService geminiService;

    @MockBean
    private RestClient restClient;

    @MockBean
    private ReviewMetrics reviewMetrics;

    @MockBean
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @MockBean
    private RestClient.RequestBodySpec requestBodySpec;

    @MockBean
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        lenient().when(reviewMetrics.startTimer()).thenReturn(mock(Timer.Sample.class));
        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("Should retry 3 times on HTTP 429 TooManyRequests and execute @Recover fallback")
    void shouldRetry3TimesOn429_AndExecuteRecover() {
        // Arrange: simulate HTTP 429 TooManyRequests on all attempts
        HttpClientErrorException.TooManyRequests rateLimitException =
                (HttpClientErrorException.TooManyRequests) HttpClientErrorException.create(
                        HttpStatusCode.valueOf(429),
                        "Too Many Requests",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                );

        when(responseSpec.body(GeminiResponseDto.class)).thenThrow(rateLimitException);

        // Act
        List<FeedbackItemDto> items = geminiService.reviewCode("class Test {}", "java");

        // Assert: RestClient was called exactly 3 times (initial + 2 retries = 3 attempts)
        verify(restClient, times(3)).post();

        // Assert: @Recover executed and returned safe fallback
        assertThat(items).hasSize(1);
        FeedbackItemDto fallback = items.get(0);
        assertThat(fallback.category()).isEqualTo("SYSTEM");
        assertThat(fallback.severity()).isEqualTo("WARNING");
        assertThat(fallback.suggestion()).contains("AI code review service is currently unavailable");

        // Verify metrics incremented for failures and fallback
        verify(reviewMetrics, times(3)).incrementGeminiFailure("rate_limit_429");
        verify(reviewMetrics).incrementGeminiFailure("retry_exhausted_fallback");
    }

    @Test
    @DisplayName("Should retry on network timeout (ResourceAccessException) and execute @Recover fallback")
    void shouldRetryOnNetworkTimeout_AndExecuteRecover() {
        // Arrange: simulate network / read timeout on all attempts
        when(responseSpec.body(GeminiResponseDto.class))
                .thenThrow(new ResourceAccessException("Read timed out after 25000ms"));

        // Act
        List<FeedbackItemDto> items = geminiService.reviewCode("def test(): pass", "python");

        // Assert: 3 attempts executed
        verify(restClient, times(3)).post();

        // Assert: @Recover returned safe fallback
        assertThat(items).hasSize(1);
        assertThat(items.get(0).category()).isEqualTo("SYSTEM");

        verify(reviewMetrics, times(3)).incrementGeminiFailure("timeout");
        verify(reviewMetrics).incrementGeminiFailure("retry_exhausted_fallback");
    }

    @Test
    @DisplayName("Should succeed on second attempt if first attempt experienced transient 500 error")
    void shouldSucceedOnSecondAttempt_WhenFirstAttemptFailsWith500() {
        // Arrange: first call fails with 500 Server Error, second call succeeds
        HttpServerErrorException serverError = new HttpServerErrorException(
                HttpStatusCode.valueOf(500),
                "Internal Server Error"
        );

        String successJson = """
                [
                  {
                    "lineNumber": 1,
                    "category": "READABILITY",
                    "severity": "INFO",
                    "suggestion": "Add docstring.",
                    "replacementCode": null
                  }
                ]
                """;

        GeminiResponseDto successResponse = new GeminiResponseDto(
                List.of(new GeminiResponseDto.Candidate(
                        new GeminiResponseDto.Content(List.of(new GeminiResponseDto.Part(successJson)), "model"),
                        "STOP"
                )),
                null,
                "gemini-2.5-flash"
        );

        when(responseSpec.body(GeminiResponseDto.class))
                .thenThrow(serverError)
                .thenReturn(successResponse);

        // Act
        List<FeedbackItemDto> items = geminiService.reviewCode("def add(a, b): return a + b", "python");

        // Assert: called exactly 2 times (1 failure + 1 successful retry)
        verify(restClient, times(2)).post();

        // Assert: parsed items successfully returned
        assertThat(items).hasSize(1);
        assertThat(items.get(0).category()).isEqualTo("READABILITY");
        assertThat(items.get(0).suggestion()).isEqualTo("Add docstring.");

        // Assert: 1 failure metric, 1 success latency recorded
        verify(reviewMetrics, times(1)).incrementGeminiFailure("server_error_5xx");
        verify(reviewMetrics, times(1)).recordGeminiLatency(any(), eq("gemini-2.5-flash"), eq("success"));
    }
}
