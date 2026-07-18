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
}
