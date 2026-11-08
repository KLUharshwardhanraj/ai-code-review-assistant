package com.aicodereviewer.entity.enums;

/**
 * Represents the allowed human decisions for a feedback item in REST requests.
 *
 * <p>This is intentionally separate from {@link FeedbackResolution} — it contains
 * only the two values a client is permitted to send. {@code PENDING} is excluded
 * because it is an internal default state, not a user action.</p>
 *
 * <p>The service layer maps this to {@link FeedbackResolution} for persistence.</p>
 */
public enum FeedbackStatus {

    ACCEPTED,
    REJECTED
}
