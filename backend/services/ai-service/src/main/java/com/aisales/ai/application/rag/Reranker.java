package com.aisales.ai.application.rag;

import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import java.util.List;

/**
 * Pluggable RAG reranker. Selected by {@code aisales.ai.rag.reranker}.
 */
public interface Reranker {

    /** Router key: {@code NONE} | {@code STUB} | {@code TEI}. */
    String name();

    /**
     * Reorders {@code candidates} by relevance to {@code query} and returns at most {@code topK}.
     */
    List<RetrievedKnowledgeChunkDto> rerank(
            String query, List<RetrievedKnowledgeChunkDto> candidates, int topK);
}
