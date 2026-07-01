package com.aisales.ai.api.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EmbeddingResponse {

    private String tenantId;
    private String collectionKey;
    private String modelName;
    private String modelProvider;
    private String providerKind;
    private int dimension;
    private List<EmbeddingVectorResponse> embeddings;
}
