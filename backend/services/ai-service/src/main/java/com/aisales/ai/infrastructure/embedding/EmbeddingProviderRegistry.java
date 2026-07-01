package com.aisales.ai.infrastructure.embedding;

import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.domain.embedding.EmbeddingProviderKind;
import com.aisales.ai.infrastructure.configuration.EmbeddingProperties;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Routes embedding requests to the correct open-source or commercial provider.
 */
@Component
@RequiredArgsConstructor
public class EmbeddingProviderRegistry {

    private final List<EmbeddingProvider> providers;
    private final EmbeddingProperties properties;

    public EmbeddingProvider resolve(EmbeddingProviderKind kind, String modelName) {
        return providers.stream()
                .filter(p -> p.kind() == kind)
                .filter(p -> p.supports(modelName))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.AI_UNAVAILABLE,
                        "No embedding provider registered for kind=%s model=%s".formatted(kind, modelName)));
    }

    public EmbeddingProvider resolveDefault() {
        return resolve(properties.getDefaultProviderKind(), properties.getDefaultModel());
    }

    public EmbeddingProvider resolve(EmbeddingProviderKind kind) {
        String model = kind == EmbeddingProviderKind.OPEN_SOURCE
                ? properties.getOpenSource().getModel()
                : properties.getCommercial().getOpenai().getModel();
        return resolve(kind, model);
    }
}
