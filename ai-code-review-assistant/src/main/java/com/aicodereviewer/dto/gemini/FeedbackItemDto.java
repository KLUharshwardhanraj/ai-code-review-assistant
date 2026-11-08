package com.aicodereviewer.dto.gemini;

/**
 * Application-level transfer object representing a single code review
 * feedback item parsed from Gemini's structured JSON output.
 *
 * <p>This record serves as the boundary between the Gemini integration layer
 * and the application service layer. The service layer maps these DTOs to
 * {@link com.aicodereviewer.entity.ReviewFeedback} JPA entities for persistence.</p>
 *
 * <h3>Schema contract</h3>
 * <p>Fields correspond exactly to the {@code responseSchema} enforced on the
 * Gemini API call. The schema guarantees that {@code category}, {@code severity},
 * and {@code suggestion} are always present; {@code lineNumber} and
 * {@code replacementCode} may be null.</p>
 *
 * <h3>Special "SYSTEM" category</h3>
 * <p>The {@code @Recover} fallback in {@code GeminiService} produces a synthetic
 * feedback item with {@code category = "SYSTEM"} to signal infrastructure failures.
 * This value is not part of the Gemini schema but is handled during entity mapping.</p>
 *
 * @param lineNumber      the source line this feedback references (null for general advice)
 * @param category        classification: BUG, EDGE_CASE, READABILITY, SECURITY, or SYSTEM (fallback)
 * @param severity        impact level: CRITICAL, WARNING, or INFO
 * @param suggestion      human-readable explanation of the issue and recommended fix
 * @param replacementCode concrete replacement snippet (null if no code change is suggested)
 */
public record FeedbackItemDto(
        Integer lineNumber,
        String category,
        String severity,
        String suggestion,
        String replacementCode
) {
}
