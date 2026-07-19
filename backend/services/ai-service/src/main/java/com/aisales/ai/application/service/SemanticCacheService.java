package com.aisales.ai.application.service;

import com.aisales.ai.domain.cache.CachedLlmResponse;
import com.aisales.ai.domain.embedding.EmbeddingBatchResult;
import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.infrastructure.configuration.SemanticCacheProperties;
import com.aisales.ai.infrastructure.embedding.EmbeddingProviderRegistry;
import com.aisales.ai.infrastructure.persistence.SemanticCacheEntry;
import com.aisales.ai.infrastructure.persistence.SemanticCacheJpaRepository;
import com.aisales.ai.infrastructure.persistence.SemanticCacheVectorRepository;
import com.aisales.common.cache.PlatformCacheService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Semantic LLM response cache. Embedding HTTP runs outside DB transactions; EMBED quota
 * is reserved around similarity lookups and puts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticCacheService {

    private static final String CACHE_NAMESPACE = "semantic-cache";

    private final SemanticCacheJpaRepository cacheRepository;
    private final SemanticCacheVectorRepository vectorRepository;
    private final EmbeddingProviderRegistry providerRegistry;
    private final SemanticCacheProperties cacheProperties;
    private final ObjectProvider<PlatformCacheService> platformCacheService;
    private final PlatformTransactionManager transactionManager;
    private final AiQuotaService aiQuotaService;
    private final TokenUsageService tokenUsageService;

    public Optional<CachedLlmResponse> get(UUID tenantId, String queryText, String model) {
        return get(tenantId, "", queryText, model);
    }

    public Optional<CachedLlmResponse> get(UUID tenantId, String promptScope, String queryText, String model) {
        if (!cacheProperties.isEnabled()) {
            return Optional.empty();
        }
        String scope = normalizeScope(promptScope);
        String cacheKey = cacheKey(tenantId, scope, queryText, model);
        Optional<CachedLlmResponse> redisHit = getFromRedis(cacheKey);
        if (redisHit.isPresent()) {
            return redisHit;
        }

        String hash = queryHash(tenantId, scope, queryText);
        Optional<SemanticCacheEntry> exact = new TransactionTemplate(transactionManager).execute(status ->
                cacheRepository.findByTenantIdAndPromptScopeAndQueryHashAndModelUsed(
                        tenantId, scope, hash, model));
        if (exact != null && exact.isPresent() && isValid(exact.get())) {
            return recordHit(exact.get(), cacheKey);
        }

        EmbeddingProvider provider = resolveProvider();
        float[] embedding = embedWithQuota(tenantId, provider, queryText, "SEMANTIC_CACHE_GET");
        Optional<UUID> similarId = new TransactionTemplate(transactionManager).execute(status ->
                vectorRepository.findMostSimilarId(
                        tenantId, scope, model, embedding, cacheProperties.getMaxCosineDistance()));
        if (similarId == null || similarId.isEmpty()) {
            return Optional.empty();
        }

        Optional<SemanticCacheEntry> similar = new TransactionTemplate(transactionManager).execute(status ->
                cacheRepository.findById(similarId.get()).filter(this::isValid));
        if (similar == null || similar.isEmpty()) {
            return Optional.empty();
        }
        return recordHit(similar.get(), cacheKey);
    }

    public void put(UUID tenantId, String queryText, CachedLlmResponse response, String model) {
        put(tenantId, "", queryText, response, model);
    }

    public void put(
            UUID tenantId, String promptScope, String queryText, CachedLlmResponse response, String model) {
        if (!cacheProperties.isEnabled()) {
            return;
        }
        String scope = normalizeScope(promptScope);
        EmbeddingProvider provider = resolveProvider();
        float[] embedding = embedWithQuota(tenantId, provider, queryText, "SEMANTIC_CACHE_PUT");
        String hash = queryHash(tenantId, scope, queryText);
        Instant expiresAt = Instant.now().plus(cacheProperties.getDefaultTtl());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            SemanticCacheEntry entry =
                    cacheRepository
                            .findByTenantIdAndPromptScopeAndQueryHashAndModelUsed(tenantId, scope, hash, model)
                            .orElseGet(() -> SemanticCacheEntry.builder()
                                    .tenantId(tenantId)
                                    .promptScope(scope)
                                    .queryHash(hash)
                                    .queryText(queryText)
                                    .modelUsed(model)
                                    .hitCount(0)
                                    .build());

            entry.setPromptScope(scope);
            entry.setQueryText(queryText);
            entry.setResponse(Map.of(
                    "content", response.getContent(),
                    "model", response.getModel(),
                    "metadata", response.getMetadata() != null ? response.getMetadata() : Map.of()));
            entry.setMetadata(response.getMetadata());
            entry.setExpiresAt(expiresAt);

            SemanticCacheEntry saved = cacheRepository.save(entry);
            vectorRepository.updateEmbedding(saved.getId(), embedding);
        });
        putInRedis(cacheKey(tenantId, scope, queryText, model), response);
        log.debug("semantic_cache_stored tenant_id={} scope={} model={}", tenantId, scope, model);
    }

    public void invalidateTenant(UUID tenantId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                cacheRepository.deleteByTenantId(tenantId));
        log.info("semantic_cache_invalidated tenant_id={}", tenantId);
    }

    public double hitRate(UUID tenantId) {
        long total = cacheRepository.countByTenantId(tenantId);
        if (total == 0) {
            return 0.0;
        }
        return (double) cacheRepository.sumHitCountByTenantId(tenantId) / total;
    }

    private float[] embedWithQuota(
            UUID tenantId, EmbeddingProvider provider, String queryText, String purpose) {
        long reserved = aiQuotaService.reserveEmbed(tenantId);
        try {
            EmbeddingBatchResult batch = provider.embedWithUsage(List.of(queryText));
            tokenUsageService.recordEmbeddingUsage(
                    tenantId,
                    provider.name().toLowerCase(Locale.ROOT),
                    provider.modelName(),
                    List.of(queryText),
                    purpose,
                    purpose,
                    batch.promptTokens());
            List<float[]> vectors = batch.vectors();
            if (vectors.isEmpty()) {
                throw new IllegalStateException("Embedding provider returned no vectors");
            }
            return vectors.getFirst();
        } finally {
            aiQuotaService.release(tenantId, AiQuotaService.OPERATION_EMBED, reserved);
        }
    }

    private Optional<CachedLlmResponse> recordHit(SemanticCacheEntry entry, String cacheKey) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entry.incrementHitCount();
            cacheRepository.save(entry);
        });
        CachedLlmResponse response = toResponse(entry);
        putInRedis(cacheKey, response);
        return Optional.of(response);
    }

    private boolean isValid(SemanticCacheEntry entry) {
        return entry.getExpiresAt() == null || entry.getExpiresAt().isAfter(Instant.now());
    }

    private Optional<CachedLlmResponse> getFromRedis(String cacheKey) {
        PlatformCacheService cache = platformCacheService.getIfAvailable();
        if (cache == null) {
            return Optional.empty();
        }
        return cache.get(CACHE_NAMESPACE, cacheKey, CachedLlmResponse.class);
    }

    private void putInRedis(String cacheKey, CachedLlmResponse response) {
        platformCacheService.ifAvailable(cache ->
                cache.put(CACHE_NAMESPACE, cacheKey, response, cacheProperties.getRedisTtl()));
    }

    private CachedLlmResponse toResponse(SemanticCacheEntry entry) {
        Map<String, Object> payload = entry.getResponse();
        return CachedLlmResponse.builder()
                .content(String.valueOf(payload.get("content")))
                .model(entry.getModelUsed())
                .metadata(entry.getMetadata())
                .build();
    }

    private EmbeddingProvider resolveProvider() {
        return providerRegistry.resolveDefault();
    }

    private String cacheKey(UUID tenantId, String promptScope, String queryText, String model) {
        return tenantId + ":" + promptScope + ":" + queryHash(tenantId, promptScope, queryText) + ":" + model;
    }

    private static String normalizeScope(String promptScope) {
        return StringUtils.hasText(promptScope) ? promptScope.trim() : "";
    }

    private static String queryHash(UUID tenantId, String promptScope, String queryText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(tenantId.toString().getBytes(StandardCharsets.UTF_8));
            digest.update(promptScope.getBytes(StandardCharsets.UTF_8));
            digest.update(queryText.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
