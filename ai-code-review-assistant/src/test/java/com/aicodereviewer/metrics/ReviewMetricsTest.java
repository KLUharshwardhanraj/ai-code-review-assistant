package com.aicodereviewer.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewMetrics Unit Tests")
class ReviewMetricsTest {

    private MeterRegistry meterRegistry;
    private ReviewMetrics reviewMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        reviewMetrics = new ReviewMetrics(meterRegistry);
    }

    @Test
    @DisplayName("Should increment submissions counter with language and submission_type tags")
    void shouldIncrementSubmissionsCounter_WithCorrectTags() {
        reviewMetrics.incrementSubmissions("JAVA", "snippet");

        Counter counter = meterRegistry.get(ReviewMetrics.METRIC_SUBMISSIONS_TOTAL)
                .tag("language", "java")
                .tag("submission_type", "SNIPPET")
                .counter();

        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should increment feedback decisions counter with decision and category tags")
    void shouldIncrementFeedbackDecisionCounter_WithCorrectTags() {
        reviewMetrics.incrementFeedbackDecision("ACCEPTED", "BUG");
        reviewMetrics.incrementFeedbackDecision("rejected", "READABILITY");

        Counter acceptedCounter = meterRegistry.get(ReviewMetrics.METRIC_DECISIONS_TOTAL)
                .tag("decision", "accepted")
                .tag("category", "BUG")
                .counter();

        Counter rejectedCounter = meterRegistry.get(ReviewMetrics.METRIC_DECISIONS_TOTAL)
                .tag("decision", "rejected")
                .tag("category", "READABILITY")
                .counter();

        assertThat(acceptedCounter.count()).isEqualTo(1.0);
        assertThat(rejectedCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should increment Gemini failures counter with error_type tag")
    void shouldIncrementGeminiFailureCounter() {
        reviewMetrics.incrementGeminiFailure("rate_limit_429");
        reviewMetrics.incrementGeminiFailure("rate_limit_429");
        reviewMetrics.incrementGeminiFailure("timeout");

        Counter rateLimitCounter = meterRegistry.get(ReviewMetrics.METRIC_GEMINI_FAILURES_TOTAL)
                .tag("error_type", "rate_limit_429")
                .counter();

        Counter timeoutCounter = meterRegistry.get(ReviewMetrics.METRIC_GEMINI_FAILURES_TOTAL)
                .tag("error_type", "timeout")
                .counter();

        assertThat(rateLimitCounter.count()).isEqualTo(2.0);
        assertThat(timeoutCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record Gemini latency timer accurately")
    void shouldRecordGeminiLatencyTimer() {
        Timer.Sample sample = reviewMetrics.startTimer();
        reviewMetrics.recordGeminiLatency(sample, "gemini-2.5-flash", "success");

        Timer timer = meterRegistry.get(ReviewMetrics.METRIC_GEMINI_LATENCY)
                .tag("model", "gemini-2.5-flash")
                .tag("status", "success")
                .timer();

        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should record review session processing timer")
    void shouldRecordSessionProcessingTimer() {
        Timer.Sample sample = reviewMetrics.startTimer();
        reviewMetrics.recordSessionProcessingTime(sample, "python", "success");

        Timer timer = meterRegistry.get(ReviewMetrics.METRIC_SESSION_PROCESSING)
                .tag("language", "python")
                .tag("status", "success")
                .timer();

        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should update pending reviews gauge value")
    void shouldUpdatePendingReviewsGauge() {
        reviewMetrics.setPendingReviewsCount(7);

        double gaugeValue = meterRegistry.get(ReviewMetrics.METRIC_PENDING_GAUGE)
                .gauge()
                .value();

        assertThat(gaugeValue).isEqualTo(7.0);
    }
}
