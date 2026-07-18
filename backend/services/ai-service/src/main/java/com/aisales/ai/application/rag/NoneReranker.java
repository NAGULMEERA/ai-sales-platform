package com.aisales.ai.application.rag;

import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Pass-through reranker (vector order unchanged). Selected when
 * {@code aisales.ai.rag.reranker=NONE}.
 */
@Component
public class NoneReranker implements Reranker {

    public static final String NAME = "NONE";

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
        return candidates.stream().limit(topK).toList();
    }
}
