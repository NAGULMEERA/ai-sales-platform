package com.aisales.ai.application.service;

import com.aisales.ai.domain.cache.CachedLlmResponse;
import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.infrastructure.configuration.SemanticCacheProperties;
import com.aisales.ai.infrastructure.embedding.EmbeddingProviderRegistry;
import com.aisales.ai.infrastructure.persistence.SemanticCacheEntry;
import com.aisales.ai.infrastructure.persistence.SemanticCacheJpaRepository;
import com.aisales.ai.infrastructure.persistence.SemanticCacheVectorRepository;
import com.aisales.common.cache.PlatformCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    @Transactional(readOnly = true)
    public Optional<CachedLlmResponse> get(UUID tenantId, String queryText, String model) {
        if (!cacheProperties.isEnabled()) {
            return Optional.empty();
        }

        String cacheKey = cacheKey(tenantId, queryText, model);
        Optional<CachedLlmResponse> redisHit = getFromRedis(cacheKey);
        if (redisHit.isPresent()) {
            return redisHit;
        }

        Optional<SemanticCacheEntry> exact = cacheRepository.findByTenantIdAndQueryHashAndModelUsed(
                tenantId, queryHash(tenantId, queryText), model);
        if (exact.isPresent() && isValid(exact.get())) {
            return recordHit(exact.get(), cacheKey);
        }

        EmbeddingProvider provider = resolveProvider();
        float[] embedding = provider.embed(List.of(queryText)).getFirst();
        Optional<UUID> similarId = vectorRepository.findMostSimilarId(
                tenantId, model, embedding, cacheProperties.getMaxCosineDistance());
        if (similarId.isEmpty()) {
            return Optional.empty();
        }

        return cacheRepository.findById(similarId.get())
                .filter(this::isValid)
                .flatMap(entry -> recordHit(entry, cacheKey));
    }

    @Transactional
    public void put(UUID tenantId, String queryText, CachedLlmResponse response, String model) {
        if (!cacheProperties.isEnabled()) {
            return;
        }

        EmbeddingProvider provider = resolveProvider();
        float[] embedding = provider.embed(List.of(queryText)).getFirst();
        String hash = queryHash(tenantId, queryText);
        Instant expiresAt = Instant.now().plus(cacheProperties.getDefaultTtl());

        SemanticCacheEntry entry = cacheRepository.findByTenantIdAndQueryHashAndModelUsed(tenantId, hash, model)
                .orElseGet(() -> SemanticCacheEntry.builder()
                        .tenantId(tenantId)
                        .queryHash(hash)
                        .queryText(queryText)
                        .modelUsed(model)
                        .hitCount(0)
                        .build());

        entry.setQueryText(queryText);
        entry.setResponse(Map.of(
                "content", response.getContent(),
                "model", response.getModel(),
                "metadata", response.getMetadata() != null ? response.getMetadata() : Map.of()));
        entry.setMetadata(response.getMetadata());
        entry.setExpiresAt(expiresAt);

        SemanticCacheEntry saved = cacheRepository.save(entry);
        vectorRepository.updateEmbedding(saved.getId(), embedding);
        putInRedis(cacheKey(tenantId, queryText, model), response);
        log.debug("semantic_cache_stored tenant_id={} model={}", tenantId, model);
    }

    @Transactional
    public void invalidateTenant(UUID tenantId) {
        cacheRepository.deleteByTenantId(tenantId);
        log.info("semantic_cache_invalidated tenant_id={}", tenantId);
    }

    public double hitRate(UUID tenantId) {
        long total = cacheRepository.countByTenantId(tenantId);
        if (total == 0) {
            return 0.0;
        }
        return (double) cacheRepository.sumHitCountByTenantId(tenantId) / total;
    }

    private Optional<CachedLlmResponse> recordHit(SemanticCacheEntry entry, String cacheKey) {
        entry.incrementHitCount();
        cacheRepository.save(entry);
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
        // Cache key uses the LLM model; vectors always come from the configured embedding provider.
        return providerRegistry.resolveDefault();
    }

    private String cacheKey(UUID tenantId, String queryText, String model) {
        return tenantId + ":" + queryHash(tenantId, queryText) + ":" + model;
    }

    private static String queryHash(UUID tenantId, String queryText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(tenantId.toString().getBytes(StandardCharsets.UTF_8));
            digest.update(queryText.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
