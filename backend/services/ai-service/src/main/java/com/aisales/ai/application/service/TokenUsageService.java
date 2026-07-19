package com.aisales.ai.application.service;

import com.aisales.ai.domain.entity.TokenUsage;
import com.aisales.ai.domain.llm.LlmCompletionResult;
import com.aisales.ai.infrastructure.persistence.TokenUsageRepository;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.observability.metrics.AiRequestMetrics;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TokenUsageService {

    private final TokenUsageRepository tokenUsageRepository;
    private final AiCostEstimator costEstimator;
    private final ObjectProvider<AiRequestMetrics> aiRequestMetrics;

    @Transactional
    public TokenUsage recordExecuteUsage(
            UUID tenantId,
            UUID executionId,
            String promptCode,
            LlmCompletionResult completion,
            String businessReference) {
        int promptTokens = completion.promptTokens() != null ? completion.promptTokens() : 0;
        int completionTokens = completion.completionTokens() != null ? completion.completionTokens() : 0;
        int totalTokens = promptTokens + completionTokens;
        String model = completion.model() != null ? completion.model() : "UNKNOWN";
        BigDecimal cost = costEstimator.estimateLlm(model, promptTokens, completionTokens);

        TokenUsage usage = tokenUsageRepository.save(TokenUsage.builder()
                .tenantId(tenantId)
                .organizationId(parseUuidOrNull(TenantContext.getOrganizationId()))
                .executionId(executionId)
                .promptCode(promptCode)
                .provider(completion.provider() != null ? completion.provider() : "UNKNOWN")
                .model(model)
                .operation("EXECUTE")
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .businessReference(businessReference)
                .estimatedCostUsd(cost)
                .createdAt(Instant.now())
                .build());

        recordMetrics(tenantId, usage.getProvider(), "EXECUTE", totalTokens);
        return usage;
    }

    /**
     * Records embedding usage for API embed, RAG index, or retrieval.
     * Token count is estimated from text length when the provider does not return usage.
     */
    @Transactional
    public TokenUsage recordEmbeddingUsage(
            UUID tenantId,
            String provider,
            String model,
            List<String> texts,
            String businessReference,
            String collectionOrPurpose) {
        return recordEmbeddingUsage(
                tenantId, provider, model, texts, businessReference, collectionOrPurpose, null);
    }

    /**
     * @param providerPromptTokens provider-reported prompt tokens when available; otherwise estimated
     */
    @Transactional
    public TokenUsage recordEmbeddingUsage(
            UUID tenantId,
            String provider,
            String model,
            List<String> texts,
            String businessReference,
            String collectionOrPurpose,
            Integer providerPromptTokens) {
        int embeddingTokens = providerPromptTokens != null && providerPromptTokens > 0
                ? providerPromptTokens
                : estimateEmbeddingTokens(texts);
        BigDecimal cost = costEstimator.estimateEmbedding(model, embeddingTokens);

        TokenUsage usage = tokenUsageRepository.save(TokenUsage.builder()
                .tenantId(tenantId)
                .organizationId(parseUuidOrNull(TenantContext.getOrganizationId()))
                .executionId(UUID.randomUUID())
                .promptCode(truncate(collectionOrPurpose != null ? collectionOrPurpose : "EMBED", 100))
                .provider(provider != null ? provider : "UNKNOWN")
                .model(model != null ? model : "UNKNOWN")
                .operation("EMBED")
                .promptTokens(embeddingTokens)
                .completionTokens(0)
                .totalTokens(embeddingTokens)
                .businessReference(businessReference)
                .estimatedCostUsd(cost)
                .createdAt(Instant.now())
                .build());

        recordMetrics(tenantId, usage.getProvider(), "EMBED", embeddingTokens);
        return usage;
    }

    static int estimateEmbeddingTokens(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return 0;
        }
        int chars = 0;
        for (String text : texts) {
            if (text != null) {
                chars += text.length();
            }
        }
        return Math.max(1, chars / 4);
    }

    private void recordMetrics(UUID tenantId, String provider, String operation, int totalTokens) {
        AiRequestMetrics metrics = aiRequestMetrics.getIfAvailable();
        if (metrics != null && totalTokens > 0) {
            metrics.recordTokens(tenantId.toString(), provider, operation, totalTokens);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "EMBED";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static UUID parseUuidOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
