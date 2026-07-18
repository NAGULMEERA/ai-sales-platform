package com.aisales.common.contracts.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Plain-text indexing companion: text → chunk → embed → pgvector.
 * Prefer {@code POST .../ingest} when the source is a Media Service object (PDF/text).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexKnowledgeDocumentRequest {

    @NotBlank
    private String text;

    /**
     * Optional size override. Units follow {@code aisales.ai.rag.chunker}:
     * characters when {@code CHAR}, approximate tokens when {@code TOKEN}.
     */
    private Integer chunkSizeChars;

    /** Optional overlap override; same unit rules as {@link #chunkSizeChars}. */
    private Integer chunkOverlapChars;
}
