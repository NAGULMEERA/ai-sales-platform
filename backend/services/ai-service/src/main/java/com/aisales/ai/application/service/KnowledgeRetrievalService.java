package com.aisales.ai.application.service;

import com.aisales.ai.application.rag.RetrieverRegistry;
import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Application facade over {@link com.aisales.ai.application.rag.Retriever}.
 * Prefer injecting {@link RetrieverRegistry} in new code (AI Gateway does).
 */
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

    private final RetrieverRegistry retrieverRegistry;

    public List<RetrievedKnowledgeChunkDto> retrieve(
            UUID knowledgeBaseId, String query, Integer topK) {
        return retrieverRegistry.resolveDefault().retrieve(knowledgeBaseId, query, topK);
    }

    public String resolveQuery(String retrievalQuery, Map<String, String> variables) {
        if (StringUtils.hasText(retrievalQuery)) {
            return retrievalQuery.trim();
        }
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        if (StringUtils.hasText(variables.get("question"))) {
            return variables.get("question").trim();
        }
        if (StringUtils.hasText(variables.get("query"))) {
            return variables.get("query").trim();
        }
        return String.join(" ", variables.values()).trim();
    }
}
