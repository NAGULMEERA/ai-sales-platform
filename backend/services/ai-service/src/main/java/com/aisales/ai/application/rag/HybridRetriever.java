package com.aisales.ai.application.rag;

import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Hybrid retrieval: dense vector + full-text keyword, fused with Reciprocal Rank Fusion (RRF).
 */
@Component
@RequiredArgsConstructor
public class HybridRetriever implements Retriever {

    private static final int RRF_K = 60;

    private final VectorRetriever vectorRetriever;
    private final KeywordRetriever keywordRetriever;

    @Override
    public String name() {
        return "HYBRID";
    }

    @Override
    public List<RetrievedKnowledgeChunkDto> retrieve(UUID knowledgeBaseId, String query, Integer topK) {
        int limit = topK != null && topK > 0 ? Math.min(topK, 20) : 5;
        int fetch = Math.min(limit * 2, 20);

        List<RetrievedKnowledgeChunkDto> vectorHits =
                vectorRetriever.retrieve(knowledgeBaseId, query, fetch);
        List<RetrievedKnowledgeChunkDto> keywordHits =
                keywordRetriever.retrieve(knowledgeBaseId, query, fetch);

        Map<UUID, Double> scores = new HashMap<>();
        Map<UUID, RetrievedKnowledgeChunkDto> byId = new LinkedHashMap<>();

        addRankScores(vectorHits, scores, byId);
        addRankScores(keywordHits, scores, byId);

        List<UUID> ranked = new ArrayList<>(scores.keySet());
        ranked.sort(Comparator
                .comparing((UUID id) -> scores.getOrDefault(id, 0.0))
                .reversed()
                .thenComparing(id -> {
                    RetrievedKnowledgeChunkDto chunk = byId.get(id);
                    return chunk != null && chunk.getDistance() != null ? chunk.getDistance() : 1.0;
                }));

        List<RetrievedKnowledgeChunkDto> result = new ArrayList<>();
        for (UUID id : ranked) {
            RetrievedKnowledgeChunkDto chunk = byId.get(id);
            if (chunk != null) {
                result.add(chunk);
            }
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private static void addRankScores(
            List<RetrievedKnowledgeChunkDto> hits,
            Map<UUID, Double> scores,
            Map<UUID, RetrievedKnowledgeChunkDto> byId) {
        for (int i = 0; i < hits.size(); i++) {
            RetrievedKnowledgeChunkDto chunk = hits.get(i);
            if (chunk.getChunkId() == null) {
                continue;
            }
            byId.putIfAbsent(chunk.getChunkId(), chunk);
            double rrf = 1.0 / (RRF_K + i + 1);
            scores.merge(chunk.getChunkId(), rrf, Double::sum);
        }
    }
}
