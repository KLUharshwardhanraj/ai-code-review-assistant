package com.aicodereviewer.entity.enums;

/**
 * Classifies the type of feedback produced by the Gemini code review.
 *
 * <p>These categories map directly to the structured JSON schema sent to Gemini,
 * ensuring consistent classification across AI responses.</p>
 *
 * <ul>
 *   <li>{@code BUG} — Logical errors, null pointer risks, off-by-one errors.</li>
 *   <li>{@code EDGE_CASE} — Unhandled boundary conditions or corner cases.</li>
 *   <li>{@code READABILITY} — Naming, formatting, code clarity improvements.</li>
 *   <li>{@code SECURITY} — Injection vulnerabilities, insecure patterns, data exposure.</li>
 *   <li>{@code PERFORMANCE} — Algorithmic inefficiencies, resource leaks, N+1 queries.</li>
 *   <li>{@code BEST_PRACTICE} — Idiomatic patterns, design principle adherence.</li>
 *   <li>{@code OTHER} — Feedback that doesn't fit the above categories.</li>
 * </ul>
 */
public enum FeedbackCategory {

    BUG,
    EDGE_CASE,
    READABILITY,
    SECURITY,
    PERFORMANCE,
    BEST_PRACTICE,
    SYSTEM,
    OTHER
}
