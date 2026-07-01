package com.aisales.ai.application.service;

import com.aisales.ai.api.request.EmbeddingRequest;
import com.aisales.ai.api.response.EmbeddingResponse;
import com.aisales.ai.api.response.EmbeddingVectorResponse;
import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.domain.embedding.EmbeddingProviderKind;
import com.aisales.ai.infrastructure.configuration.EmbeddingProperties;
import com.aisales.ai.infrastructure.embedding.EmbeddingProviderRegistry;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingApplicationService {

    private final EmbeddingProviderRegistry providerRegistry;
    private final EmbeddingProperties properties;

    public EmbeddingResponse embed(EmbeddingRequest request) {
        if (request.getTexts() == null || request.getTexts().isEmpty()) {
            throw new ValidationException("At least one text is required");
        }

        EmbeddingProviderKind kind = resolveKind(request);
        String modelName = resolveModelName(request, kind);
        EmbeddingProvider provider = providerRegistry.resolve(kind, modelName);

        List<float[]> vectors = provider.embed(request.getTexts());
        List<EmbeddingVectorResponse> results = new ArrayList<>(vectors.size());

        for (int i = 0; i < vectors.size(); i++) {
            String text = request.getTexts().get(i);
            results.add(EmbeddingVectorResponse.builder()
                    .index(i)
                    .embedding(vectors.get(i))
                    .contentHash(sha256(text))
                    .build());
        }

        return EmbeddingResponse.builder()
                .tenantId(TenantContext.getTenantId())
                .collectionKey(request.getCollectionKey())
                .modelName(provider.modelName())
                .modelProvider(kind == EmbeddingProviderKind.OPEN_SOURCE ? "baai" : "openai")
                .providerKind(kind.name())
                .dimension(provider.dimension())
                .embeddings(results)
                .build();
    }

    private EmbeddingProviderKind resolveKind(EmbeddingRequest request) {
        if (request.getProviderKind() != null) {
            return request.getProviderKind();
        }
        return properties.getDefaultProviderKind();
    }

    private String resolveModelName(EmbeddingRequest request, EmbeddingProviderKind kind) {
        if (request.getModelName() != null) {
            return request.getModelName();
        }
        return kind == EmbeddingProviderKind.OPEN_SOURCE
                ? properties.getOpenSource().getModel()
                : properties.getCommercial().getOpenai().getModel();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
