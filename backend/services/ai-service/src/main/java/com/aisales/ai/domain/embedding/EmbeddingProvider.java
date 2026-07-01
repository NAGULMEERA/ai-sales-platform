package com.aisales.ai.domain.embedding;

import java.util.List;

/**
 * Pluggable embedding backend (BGE-M3 via TEI, OpenAI, etc.).
 */
public interface EmbeddingProvider {

    EmbeddingProviderKind kind();

    String modelName();

    int dimension();

    boolean supports(String modelName);

    List<float[]> embed(List<String> texts);
}
