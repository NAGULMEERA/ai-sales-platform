package com.aisales.common.contracts.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Triggers download from Media Service + extract + index into RAG.
 * Optional chunk overrides follow {@code aisales.ai.rag.chunker} unit rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestKnowledgeDocumentRequest {

    private Integer chunkSizeChars;

    private Integer chunkOverlapChars;

    /**
     * Optional extractor override: {@code TEXT} | {@code PDF} | {@code AUTO}.
     * Null uses {@code aisales.ai.rag.extractor}.
     */
    private String extractor;
}
