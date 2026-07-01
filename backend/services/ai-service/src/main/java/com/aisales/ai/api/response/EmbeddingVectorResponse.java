package com.aisales.ai.api.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmbeddingVectorResponse {

    private int index;
    private float[] embedding;
    private String contentHash;
}
