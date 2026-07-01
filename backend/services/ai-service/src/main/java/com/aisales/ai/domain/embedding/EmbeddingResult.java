package com.aisales.ai.domain.embedding;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EmbeddingResult {

    String modelName;
    String modelProvider;
    EmbeddingProviderKind providerKind;
    int dimension;
    String modelVersion;
    String contentHash;
    float[] vector;
}
