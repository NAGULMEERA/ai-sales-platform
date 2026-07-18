package com.aisales.lead.domain.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Jackson DTO for classpath / plugin pipeline graph metadata.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PipelineTemplateDocument(
        String code,
        String name,
        String description,
        List<StageDocument> stages,
        List<TransitionDocument> transitions
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StageDocument(String status, String displayName, int order, boolean terminal) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TransitionDocument(String from, List<String> to) {
    }
}
