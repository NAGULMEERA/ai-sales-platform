package com.aisales.ai.application.rag;

import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import java.util.List;
import java.util.UUID;

/**
 * RAG retrieval port. AI Gateway depends on this interface, not on vector/SQL details.
 */
public interface Retriever {

    /** Stable key such as {@code VECTOR}, {@code KEYWORD}, {@code HYBRID}. */
    String name();

    List<RetrievedKnowledgeChunkDto> retrieve(UUID knowledgeBaseId, String query, Integer topK);
}
