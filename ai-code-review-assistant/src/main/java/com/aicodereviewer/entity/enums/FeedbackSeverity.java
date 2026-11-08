package com.aicodereviewer.entity.enums;

/**
 * Indicates the severity or impact level of a review feedback item.
 *
 * <p>Severity guides prioritization in the UI — critical and high items
 * are surfaced prominently to ensure they are addressed first.</p>
 *
 * <ul>
 *   <li>{@code CRITICAL} — Must fix: crashes, data loss, security breaches.</li>
 *   <li>{@code HIGH} — Should fix: significant bugs, major performance issues.</li>
 *   <li>{@code MEDIUM} — Recommended: moderate improvements, code smells.</li>
 *   <li>{@code LOW} — Nice to have: minor style or readability tweaks.</li>
 *   <li>{@code INFO} — Informational: observations, notes, no action needed.</li>
 * </ul>
 */
public enum FeedbackSeverity {

    CRITICAL,
    HIGH,
    WARNING,
    MEDIUM,
    LOW,
    INFO
}
