package com.aisales.ai.application.rag;

import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Free local lexical + distance blend for offline RAG. Selected when
 * {@code aisales.ai.rag.reranker=STUB}. Swap to {@code TEI} in prod for cross-encoder quality.
 */
@Component
public class StubReranker implements Reranker {

    public static final String NAME = "STUB";

    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<RetrievedKnowledgeChunkDto> rerank(
            String query, List<RetrievedKnowledgeChunkDto> candidates, int topK) {
        if (candidates == null || candidates.isEmpty() || topK <= 0) {
            return List.of();
        }
        Set<String> queryTokens = tokens(query);
        List<Scored> scored = new ArrayList<>(candidates.size());
        for (RetrievedKnowledgeChunkDto chunk : candidates) {
            double lexical = jaccard(queryTokens, tokens(chunk.getContent()));
            double distanceScore = 1.0 / (1.0 + (chunk.getDistance() != null ? chunk.getDistance() : 1.0));
            double score = (lexical * 0.7) + (distanceScore * 0.3);
            scored.add(new Scored(chunk, score));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        return scored.stream()
                .limit(topK)
                .map(s -> {
                    RetrievedKnowledgeChunkDto c = s.chunk();
                    c.setScore(s.score());
                    return c;
                })
                .toList();
    }

    private static Set<String> tokens(String text) {
        Set<String> tokens = new HashSet<>();
        if (!StringUtils.hasText(text)) {
            return tokens;
        }
        Matcher matcher = TOKEN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int intersection = 0;
        for (String t : a) {
            if (b.contains(t)) {
                intersection++;
            }
        }
        int union = a.size() + b.size() - intersection;
        return union == 0 ? 0 : (double) intersection / union;
    }

    private record Scored(RetrievedKnowledgeChunkDto chunk, double score) {
    }
}
