package com.aicodereviewer.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Dedicated telemetry and metrics component for the AI Code Review Assistant.
 */
@Component
public class ReviewMetrics {

    private static final Logger log = LoggerFactory.getLogger(ReviewMetrics.class);

    public static final String METRIC_SUBMISSIONS_TOTAL = "ai.review.submissions.total";
    public static final String METRIC_DECISIONS_TOTAL = "ai.review.feedback.decisions.total";
    public static final String METRIC_GEMINI_FAILURES_TOTAL = "ai.gemini.api.failures.total";
    public static final String METRIC_GEMINI_LATENCY = "ai.gemini.api.latency";
    public static final String METRIC_SESSION_PROCESSING = "ai.review.session.processing.time";
    public static final String METRIC_PENDING_GAUGE = "ai.review.pending.gauge";

    private final MeterRegistry meterRegistry;
    private final AtomicLong pendingReviewsGauge = new AtomicLong(0);

    public ReviewMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        // Register gauge for pending reviews
        try {
            meterRegistry.gauge(METRIC_PENDING_GAUGE, pendingReviewsGauge);
        } catch (Exception ex) {
            log.warn("Failed to register metric gauge {}: {}", METRIC_PENDING_GAUGE, ex.getMessage());
        }
    }

    /**
     * Starts a timer sample to measure an elapsed duration.
     *
     * @return a started {@link Timer.Sample}
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Increments the total code review submissions counter.
     *
     * @param language       the programming language (e.g. "java", "python")
     * @param submissionType the submission type (SNIPPET or DIFF)
     */
    public void incrementSubmissions(String language, String submissionType) {
        try {
            Counter.builder(METRIC_SUBMISSIONS_TOTAL)
                    .description("Total number of code review submissions received")
                    .tags(
                            "language", language != null ? language.toLowerCase() : "unknown",
                            "submission_type", submissionType != null ? submissionType.toUpperCase() : "SNIPPET"
                    )
                    .register(meterRegistry)
                    .increment();
        } catch (Exception ex) {
            log.warn("Failed to increment metric {}: {}", METRIC_SUBMISSIONS_TOTAL, ex.getMessage());
        }
    }

    /**
     * Increments the human-in-the-loop feedback decisions counter.
     *
     * @param decision human decision ("accepted" or "rejected")
     * @param category feedback category ("BUG", "SECURITY", "READABILITY", etc.)
     */
    public void incrementFeedbackDecision(String decision, String category) {
        try {
            Counter.builder(METRIC_DECISIONS_TOTAL)
                    .description("Total number of human-in-the-loop review feedback decisions")
                    .tags(
                            "decision", decision != null ? decision.toLowerCase() : "unknown",
                            "category", category != null ? category.toUpperCase() : "OTHER"
                    )
                    .register(meterRegistry)
                    .increment();
        } catch (Exception ex) {
            log.warn("Failed to increment metric {}: {}", METRIC_DECISIONS_TOTAL, ex.getMessage());
        }
    }

    /**
     * Increments the Gemini API failures counter.
     *
     * @param errorType categorized failure reason (e.g. "rate_limit_429", "server_error_5xx", "timeout", "json_parse_error")
     */
    public void incrementGeminiFailure(String errorType) {
        try {
            Counter.builder(METRIC_GEMINI_FAILURES_TOTAL)
                    .description("Total number of Gemini API communication and parsing failures")
                    .tag("error_type", errorType != null ? errorType : "unknown")
                    .register(meterRegistry)
                    .increment();
        } catch (Exception ex) {
            log.warn("Failed to increment metric {}: {}", METRIC_GEMINI_FAILURES_TOTAL, ex.getMessage());
        }
    }

    /**
     * Stops the timer sample and records Gemini API call duration with percentiles.
     *
     * @param sample the timer sample started before the API call
     * @param model  the Gemini model identifier
     * @param status the outcome status ("success" or "error")
     */
    public void recordGeminiLatency(Timer.Sample sample, String model, String status) {
        if (sample == null) {
            return;
        }
        try {
            Timer timer = Timer.builder(METRIC_GEMINI_LATENCY)
                    .description("Turnaround latency of requests to the Google Gemini API")
                    .tags("model", model != null ? model : "unknown", "status", status != null ? status : "unknown")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .publishPercentileHistogram()
                    .minimumExpectedValue(Duration.ofMillis(100))
                    .maximumExpectedValue(Duration.ofSeconds(30))
                    .register(meterRegistry);

            sample.stop(timer);
        } catch (Exception ex) {
            log.warn("Failed to record metric {}: {}", METRIC_GEMINI_LATENCY, ex.getMessage());
        }
    }

    /**
     * Stops the timer sample and records end-to-end review session processing duration.
     *
     * @param sample   the timer sample started at the beginning of service processing
     * @param language the programming language of the session
     * @param status   outcome status ("success" or "failure")
     */
    public void recordSessionProcessingTime(Timer.Sample sample, String language, String status) {
        if (sample == null) {
            return;
        }
        try {
            Timer timer = Timer.builder(METRIC_SESSION_PROCESSING)
                    .description("End-to-end processing time for a code review session")
                    .tags("language", language != null ? language.toLowerCase() : "unknown", "status", status != null ? status : "unknown")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry);

            sample.stop(timer);
        } catch (Exception ex) {
            log.warn("Failed to record metric {}: {}", METRIC_SESSION_PROCESSING, ex.getMessage());
        }
    }

    /**
     * Updates the gauge value tracking pending reviews count.
     *
     * @param count current number of pending items
     */
    public void setPendingReviewsCount(long count) {
        pendingReviewsGauge.set(count);
    }
}
