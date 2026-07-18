package com.aisales.ai.application.rag;

import com.aisales.ai.infrastructure.configuration.RagProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Character-window chunker. Selected when {@code aisales.ai.rag.chunker=CHAR}.
 */
@Component
@RequiredArgsConstructor
public class CharWindowChunker implements ChunkerStrategy {

    public static final String NAME = "CHAR";

    private final RagProperties properties;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<String> chunk(String text, Integer sizeOverride, Integer overlapOverride) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        int size = sizeOverride != null && sizeOverride > 0
                ? sizeOverride
                : properties.getCharWindow().getChunkSize();
        int overlap = overlapOverride != null && overlapOverride >= 0
                ? overlapOverride
                : properties.getCharWindow().getOverlap();
        overlap = Math.min(overlap, Math.max(size - 1, 0));

        if (normalized.length() <= size) {
            return List.of(normalized);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + size, normalized.length());
            chunks.add(normalized.substring(start, end).trim());
            if (end >= normalized.length()) {
                break;
            }
            start = end - overlap;
        }
        return chunks.stream().filter(StringUtils::hasText).toList();
    }
}
