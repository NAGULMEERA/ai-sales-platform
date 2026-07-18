package com.aisales.ai.infrastructure.embedding;

import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.domain.embedding.EmbeddingProviderKind;
import com.aisales.ai.infrastructure.configuration.EmbeddingProperties;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.model.ErrorCode;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Routes embedding requests to the configured provider.
 * Switch with {@code aisales.ai.embedding.provider} only (STUB | TEI | OPENAI).
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

    /**
     * Platform default from {@code aisales.ai.embedding.provider}.
     */
    public EmbeddingProvider resolveDefault() {
        return resolveByName(properties.getProvider());
    }

    public EmbeddingProvider resolveByName(String providerName) {
        if (!StringUtils.hasText(providerName)) {
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE, "aisales.ai.embedding.provider is not set");
        }
        String key = providerName.trim().toUpperCase(Locale.ROOT);
        return providers.stream()
                .filter(p -> key.equals(p.name().toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.AI_UNAVAILABLE,
                        "No embedding provider registered for provider="
                                + key
                                + ". Available: "
                                + availableNames()
                                + ". Enable the matching bean (stub / open-source / commercial) and set "
                                + "aisales.ai.embedding.provider."));
    }

    public EmbeddingProvider resolve(EmbeddingProviderKind kind) {
        String model = kind == EmbeddingProviderKind.OPEN_SOURCE
                ? properties.getOpenSource().getModel()
                : properties.getCommercial().getOpenai().getModel();
        return resolve(kind, model);
    }

    private String availableNames() {
        return providers.stream()
                .map(EmbeddingProvider::name)
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
