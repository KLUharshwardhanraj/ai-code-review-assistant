package com.aicodereviewer.dto.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponseDto(
        List<Candidate> candidates,
        UsageMetadata usageMetadata,
        String modelVersion
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(
            Content content,
            String finishReason
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(
            List<Part> parts,
            String role
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UsageMetadata(
            Integer promptTokenCount,
            Integer candidatesTokenCount,
            Integer totalTokenCount
    ) {
    }
}