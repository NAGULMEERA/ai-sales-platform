package com.aisales.ai.api.request;

import com.aisales.ai.domain.embedding.EmbeddingProviderKind;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class EmbeddingRequest {

    /** Business collection: knowledge_base, property_search, etc. */
    private String collectionKey;

    /**
     * Optional override of {@code aisales.ai.embedding.provider}.
     * Null uses the configured plug/flag default (STUB | TEI | OPENAI).
     */
    private EmbeddingProviderKind providerKind;

    /** Override model; null uses provider default for the kind. */
    private String modelName;

    @NotEmpty
    @Size(max = 100)
    private List<@Size(max = 32000) String> texts;
}
