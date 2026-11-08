package com.aicodereviewer.entity.enums;

/**
 * Represents the human review decision on a single feedback item.
 *
 * <p>This is the core of the "human-in-the-loop" workflow — no AI suggestion
 * is applied automatically. The user must explicitly accept or reject each item.</p>
 *
 * <ul>
 *   <li>{@code PENDING} — Default state; user has not yet reviewed this item.</li>
 *   <li>{@code ACCEPTED} — User agrees with the AI suggestion.</li>
 *   <li>{@code REJECTED} — User disagrees or deems the suggestion irrelevant.</li>
 * </ul>
 */
public enum FeedbackResolution {

    PENDING,
    ACCEPTED,
    REJECTED
}
