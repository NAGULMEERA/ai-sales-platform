package com.aisales.ai.application.rag;

import com.aisales.ai.infrastructure.configuration.RagProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Approximate token-window chunker (whitespace / punctuation tokens, sentence-aware ends).
 * Selected when {@code aisales.ai.rag.chunker=TOKEN}. No paid tokenizer dependency —
 * swap later if a vendor tokenizer is required.
 */
@Component
@RequiredArgsConstructor
public class TokenWindowChunker implements ChunkerStrategy {

    public static final String NAME = "TOKEN";

    private static final Pattern TOKEN = Pattern.compile("\\S+");
    private static final Pattern SENTENCE_END = Pattern.compile(".*[.!?][\"']?$");

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
        List<String> tokens = tokenize(normalized);
        if (tokens.isEmpty()) {
            return List.of();
        }

        int size = sizeOverride != null && sizeOverride > 0
                ? sizeOverride
                : properties.getTokenWindow().getChunkSize();
        int overlap = overlapOverride != null && overlapOverride >= 0
                ? overlapOverride
                : properties.getTokenWindow().getOverlap();
        overlap = Math.min(overlap, Math.max(size - 1, 0));

        if (tokens.size() <= size) {
            return List.of(String.join(" ", tokens));
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < tokens.size()) {
            int end = Math.min(start + size, tokens.size());
            end = preferSentenceBoundary(tokens, start, end);
            chunks.add(String.join(" ", tokens.subList(start, end)));
            if (end >= tokens.size()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks.stream().filter(StringUtils::hasText).toList();
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    /**
     * If the hard cut lands mid-sentence, back up to the last sentence-ending token
     * within the final 25% of the window (when that still leaves content).
     */
    private static int preferSentenceBoundary(List<String> tokens, int start, int end) {
        if (end >= tokens.size() || end - start < 4) {
            return end;
        }
        int minEnd = start + Math.max(1, (int) Math.ceil((end - start) * 0.75));
        for (int i = end - 1; i >= minEnd; i--) {
            if (SENTENCE_END.matcher(tokens.get(i)).matches()) {
                return i + 1;
            }
        }
        return end;
    }
}
