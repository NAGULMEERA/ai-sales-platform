package com.aisales.ai.domain.embedding;

import java.util.List;

/**
 * Pluggable embedding backend. Selected by {@code aisales.ai.embedding.provider}
 * ({@code STUB} | {@code TEI} | {@code OPENAI}).
 */
public interface EmbeddingProvider {

    /**
     * Router key matching {@code aisales.ai.embedding.provider} (e.g. STUB, TEI, OPENAI).
     */
    String name();

    EmbeddingProviderKind kind();

    String modelName();

    int dimension();

    boolean supports(String modelName);

    List<float[]> embed(List<String> texts);

    /**
     * Embed with optional provider token usage. Default wraps {@link #embed(List)} with null usage.
     */
    default EmbeddingBatchResult embedWithUsage(List<String> texts) {
        return new EmbeddingBatchResult(embed(texts), null);
    }
}
