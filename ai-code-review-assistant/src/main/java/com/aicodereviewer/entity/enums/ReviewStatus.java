package com.aicodereviewer.entity.enums;

/**
 * Represents the lifecycle state of a {@link com.aicodereviewer.entity.ReviewSession}.
 *
 * <p>State transitions follow this flow:</p>
 * <pre>
 *   PENDING  →  IN_REVIEW  →  COMPLETED
 *      │                          ▲
 *      └──────── FAILED ─────────┘ (retry may re-enter PENDING)
 * </pre>
 *
 * <ul>
 *   <li>{@code PENDING} — Session created, waiting for Gemini API processing.</li>
 *   <li>{@code IN_REVIEW} — Gemini feedback received; user is reviewing items.</li>
 *   <li>{@code COMPLETED} — All feedback items have been resolved (accepted/rejected).</li>
 *   <li>{@code FAILED} — Gemini API call failed after all retry attempts.</li>
 * </ul>
 */
public enum ReviewStatus {

    PENDING,
    IN_REVIEW,
    COMPLETED,
    FAILED
}
