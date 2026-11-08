package com.aicodereviewer.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Distinguishes between the two types of code submissions.
 *
 * <ul>
 *   <li>{@code SNIPPET} — A standalone source code fragment (default).</li>
 *   <li>{@code DIFF} — A Git unified diff (e.g., output of {@code git diff}).</li>
 * </ul>
 */
public enum SubmissionType {

    SNIPPET,
    DIFF;

    @JsonCreator
    public static SubmissionType fromString(String value) {
        if (value == null || value.isBlank()) {
            return SNIPPET;
        }
        for (SubmissionType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return SNIPPET;
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}