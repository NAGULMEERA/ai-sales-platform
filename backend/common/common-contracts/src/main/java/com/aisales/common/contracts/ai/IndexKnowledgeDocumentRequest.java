package com.aisales.common.contracts.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    // Bounded to prevent a single request from generating an unbounded number of chunks /
    // embedding-provider calls; the AI budget reservation for indexing uses a fixed estimate
    // that assumes documents of a reasonable size (see AiQuotaProperties.reserveEmbedTokens).
    @NotBlank
    @Size(max = 500_000)
    private String text;

    /**
     * Optional size override. Units follow {@code aisales.ai.rag.chunker}:
     * characters when {@code CHAR}, approximate tokens when {@code TOKEN}.
     */
    @Min(50)
    @Max(10_000)
    private Integer chunkSizeChars;

    /** Optional overlap override; same unit rules as {@link #chunkSizeChars}. */
    @Min(0)
    @Max(3_000)
    private Integer chunkOverlapChars;
}
