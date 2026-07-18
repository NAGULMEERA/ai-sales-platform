package com.aisales.ai.application.rag;

import com.aisales.ai.infrastructure.configuration.RagProperties;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.model.ErrorCode;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Routes indexing text to the configured chunker.
 * Switch with {@code aisales.ai.rag.chunker} only ({@code CHAR} | {@code TOKEN}).
 */
@Component
@RequiredArgsConstructor
public class TextChunker {

    private final List<ChunkerStrategy> strategies;
    private final RagProperties properties;

    public String name() {
        return resolve().name();
    }

    public List<String> chunk(String text, Integer sizeOverride, Integer overlapOverride) {
        return resolve().chunk(text, sizeOverride, overlapOverride);
    }

    ChunkerStrategy resolve() {
        String configured = properties.getChunker();
        if (!StringUtils.hasText(configured)) {
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE, "aisales.ai.rag.chunker is not set");
        }
        String key = configured.trim().toUpperCase(Locale.ROOT);
        return strategies.stream()
                .filter(s -> key.equals(s.name().toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.AI_UNAVAILABLE,
                        "No chunker registered for aisales.ai.rag.chunker="
                                + key
                                + ". Available: "
                                + strategies.stream()
                                        .map(ChunkerStrategy::name)
                                        .sorted()
                                        .collect(Collectors.joining(", "))));
    }
}
