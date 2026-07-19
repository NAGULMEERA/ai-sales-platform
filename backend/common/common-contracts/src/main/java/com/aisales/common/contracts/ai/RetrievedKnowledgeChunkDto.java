package com.aisales.common.contracts.ai;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedKnowledgeChunkDto {

    private UUID chunkId;
    private UUID documentId;
    private int chunkIndex;
    private String content;
    /** Cosine distance (lower is more similar). */
    private Double distance;
    /** Optional rerank relevance (higher is better); set when a reranker runs. */
    private Double score;
}
