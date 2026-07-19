package com.aisales.ai.application.service;

import com.aisales.ai.api.request.EmbeddingRequest;
import com.aisales.ai.api.response.EmbeddingResponse;
import com.aisales.ai.api.response.EmbeddingVectorResponse;
import com.aisales.ai.domain.embedding.EmbeddingBatchResult;
import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.domain.embedding.EmbeddingProviderKind;
import com.aisales.ai.infrastructure.configuration.EmbeddingProperties;
import com.aisales.ai.infrastructure.embedding.EmbeddingProviderRegistry;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.ValidationException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EmbeddingApplicationService {

    private final EmbeddingProviderRegistry providerRegistry;
    private final EmbeddingProperties properties;
    private final AiQuotaService aiQuotaService;
    private final TokenUsageService tokenUsageService;

    public EmbeddingResponse embed(EmbeddingRequest request) {
        if (request.getTexts() == null || request.getTexts().isEmpty()) {
            throw new ValidationException("At least one text is required");
        }

        UUID tenantId = requireTenantId();
        aiQuotaService.assertWithinDailyBudget(tenantId, AiQuotaService.OPERATION_EMBED);

        EmbeddingProvider provider = resolveProvider(request);
        EmbeddingBatchResult batch = provider.embedWithUsage(request.getTexts());
        List<float[]> vectors = batch.vectors();
        List<EmbeddingVectorResponse> results = new ArrayList<>(vectors.size());

        for (int i = 0; i < vectors.size(); i++) {
            String text = request.getTexts().get(i);
            results.add(EmbeddingVectorResponse.builder()
                    .index(i)
                    .embedding(vectors.get(i))
                    .contentHash(sha256(text))
                    .build());
        }

        String providerLabel = provider.name().toLowerCase(Locale.ROOT);
        tokenUsageService.recordEmbeddingUsage(
                tenantId,
                providerLabel,
                provider.modelName(),
                request.getTexts(),
                request.getCollectionKey(),
                request.getCollectionKey() != null ? request.getCollectionKey() : "EMBED",
                batch.promptTokens());

        return EmbeddingResponse.builder()
                .tenantId(TenantContext.getTenantId())
                .collectionKey(request.getCollectionKey())
                .modelName(provider.modelName())
                .modelProvider(providerLabel)
                .providerKind(provider.kind().name())
                .dimension(provider.dimension())
                .embeddings(results)
                .build();
    }

    private EmbeddingProvider resolveProvider(EmbeddingRequest request) {
        if (request.getProviderKind() == null && !StringUtils.hasText(request.getModelName())) {
            return providerRegistry.resolveDefault();
        }
        EmbeddingProviderKind kind = request.getProviderKind() != null
                ? request.getProviderKind()
                : properties.getDefaultProviderKind();
        String modelName = StringUtils.hasText(request.getModelName())
                ? request.getModelName()
                : (kind == EmbeddingProviderKind.OPEN_SOURCE
                        ? properties.getOpenSource().getModel()
                        : properties.getCommercial().getOpenai().getModel());
        return providerRegistry.resolve(kind, modelName);
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
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
