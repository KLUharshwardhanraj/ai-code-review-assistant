package com.aicodereviewer.dto.gemini;

import java.util.List;
import java.util.Map;

/**
 * Strongly-typed representation of the outbound request body sent to the
 * Gemini {@code generateContent} endpoint.
 *
 * <p>Uses nested records to mirror the Gemini API JSON contract exactly.
 * All field names are camelCase to match the Gemini API convention; the
 * dedicated {@code geminiObjectMapper} (which uses default naming) handles
 * serialization correctly.</p>
 *
 * <h3>Structure</h3>
 * <pre>
 * {
 *   "systemInstruction": { "parts": [{"text": "..."}] },
 *   "contents":          [{ "parts": [{"text": "..."}] }],
 *   "generationConfig":  {
 *     "responseMimeType": "application/json",
 *     "responseSchema":   { ... }
 *   }
 * }
 * </pre>
 *
 * @param systemInstruction the system-level prompt framing the model's behavior
 * @param contents          the user's code submission wrapped in Content/Part
 * @param generationConfig  controls output format (JSON schema enforcement)
 */
public record GeminiRequestDto(
        Content systemInstruction,
        List<Content> contents,
        GenerationConfig generationConfig
) {

    /**
     * A content block containing one or more textual parts.
     * Used for both the system instruction and user message.
     *
     * @param parts list of text parts
     */
    public record Content(List<Part> parts) {

        /**
         * Factory: creates a Content with a single text part.
         */
        public static Content of(String text) {
            return new Content(List.of(new Part(text)));
        }
    }

    /**
     * A single textual segment within a {@link Content} block.
     *
     * @param text the text content
     */
    public record Part(String text) {
    }

    /**
     * Generation configuration that enforces structured JSON output.
     *
     * @param responseMimeType must be {@code "application/json"} for structured output
     * @param responseSchema   the JSON Schema object constraining the response structure
     */
    public record GenerationConfig(
            String responseMimeType,
            Map<String, Object> responseSchema
    ) {
    }
}
