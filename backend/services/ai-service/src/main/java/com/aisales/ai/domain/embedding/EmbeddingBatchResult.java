package com.aisales.ai.domain.embedding;

import java.util.List;

/**
 * Embedding vectors plus optional provider-reported prompt token usage.
 */
public record EmbeddingBatchResult(List<float[]> vectors, Integer promptTokens) {
}
