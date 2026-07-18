package com.aisales.ai.application.service;

import com.aisales.ai.domain.cache.CachedLlmResponse;
import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.infrastructure.configuration.SemanticCacheProperties;
import com.aisales.ai.infrastructure.embedding.EmbeddingProviderRegistry;
import com.aisales.ai.infrastructure.persistence.SemanticCacheEntry;
import com.aisales.ai.infrastructure.persistence.SemanticCacheJpaRepository;
import com.aisales.ai.infrastructure.persistence.SemanticCacheVectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticCacheServiceTest {

    @Mock
    private SemanticCacheJpaRepository cacheRepository;

    @Mock
    private SemanticCacheVectorRepository vectorRepository;

    @Mock
    private EmbeddingProviderRegistry providerRegistry;

    @Mock
    private EmbeddingProvider embeddingProvider;

    @Mock
    private ObjectProvider<com.aisales.common.cache.PlatformCacheService> platformCacheService;

    private SemanticCacheService semanticCacheService;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SemanticCacheProperties cacheProperties = new SemanticCacheProperties();
        semanticCacheService = new SemanticCacheService(
                cacheRepository,
                vectorRepository,
                providerRegistry,
                cacheProperties,
                platformCacheService);
    }

    @Test
    void shouldReturnExactHashHit() {
        SemanticCacheEntry entry = SemanticCacheEntry.builder()
                .tenantId(tenantId)
                .queryHash("abc")
                .queryText("hello")
                .modelUsed("BAAI/bge-m3")
                .response(Map.of("content", "cached answer", "model", "BAAI/bge-m3"))
                .expiresAt(Instant.now().plusSeconds(3600))
                .hitCount(0)
                .build();

        when(cacheRepository.findByTenantIdAndQueryHashAndModelUsed(eq(tenantId), any(), eq("BAAI/bge-m3")))
                .thenReturn(Optional.of(entry));
        when(cacheRepository.save(entry)).thenReturn(entry);

        Optional<CachedLlmResponse> result = semanticCacheService.get(tenantId, "hello", "BAAI/bge-m3");

        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("cached answer");
        verify(cacheRepository).save(entry);
    }

    @Test
    void shouldStoreResponseWithEmbedding() {
        when(providerRegistry.resolveDefault()).thenReturn(embeddingProvider);
        when(embeddingProvider.embed(List.of("query"))).thenReturn(List.of(new float[] {0.1f, 0.2f}));
        when(cacheRepository.findByTenantIdAndQueryHashAndModelUsed(eq(tenantId), any(), eq("BAAI/bge-m3")))
                .thenReturn(Optional.empty());
        when(cacheRepository.save(any())).thenAnswer(invocation -> {
            SemanticCacheEntry saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        semanticCacheService.put(
                tenantId,
                "query",
                CachedLlmResponse.builder().content("answer").model("BAAI/bge-m3").build(),
                "BAAI/bge-m3");

        verify(vectorRepository).updateEmbedding(any(), any());
    }
}
