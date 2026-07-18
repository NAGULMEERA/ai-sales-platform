package com.aisales.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.aisales.ai.application.rag.NoneReranker;
import com.aisales.ai.application.rag.RerankerRegistry;
import com.aisales.ai.application.rag.StubReranker;
import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.domain.entity.KnowledgeBase;
import com.aisales.ai.infrastructure.configuration.RagProperties;
import com.aisales.ai.infrastructure.embedding.EmbeddingProviderRegistry;
import com.aisales.ai.infrastructure.persistence.KnowledgeBaseRepository;
import com.aisales.ai.infrastructure.persistence.KnowledgeChunkVectorRepository;
import com.aisales.common.contracts.ai.KnowledgeBaseStatus;
import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import com.aisales.common.core.util.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class KnowledgeRetrievalServiceTest {

    @Mock private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock private KnowledgeChunkVectorRepository knowledgeChunkVectorRepository;
    @Mock private EmbeddingProviderRegistry embeddingProviderRegistry;
    @Mock private EmbeddingProvider embeddingProvider;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private TokenUsageService tokenUsageService;

    private KnowledgeRetrievalService service;
    private UUID tenantId;
    private UUID knowledgeBaseId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        knowledgeBaseId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        RagProperties ragProperties = new RagProperties();
        ragProperties.setReranker("STUB");
        ragProperties.getRerank().setCandidateMultiplier(3);
        RerankerRegistry rerankerRegistry =
                new RerankerRegistry(List.of(new NoneReranker(), new StubReranker()), ragProperties);
        service = new KnowledgeRetrievalService(
                knowledgeBaseRepository,
                knowledgeChunkVectorRepository,
                embeddingProviderRegistry,
                transactionManager,
                tokenUsageService,
                rerankerRegistry,
                ragProperties);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldResolveQueryFromVariables() {
        assertThat(service.resolveQuery(null, Map.of("question", "how long warranty?")))
                .isEqualTo("how long warranty?");
        assertThat(service.resolveQuery("explicit", Map.of("question", "ignored")))
                .isEqualTo("explicit");
    }

    @Test
    void shouldRetrieveSimilarChunks() {
        when(knowledgeBaseRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, knowledgeBaseId))
                .thenReturn(Optional.of(KnowledgeBase.builder()
                        .id(knowledgeBaseId)
                        .tenantId(tenantId)
                        .code("FAQ")
                        .name("FAQ")
                        .status(KnowledgeBaseStatus.ACTIVE)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build()));
        when(embeddingProviderRegistry.resolveDefault()).thenReturn(embeddingProvider);
        when(embeddingProvider.name()).thenReturn("STUB");
        when(embeddingProvider.modelName()).thenReturn("stub-embedding-1024");
        float[] vector = new float[1024];
        when(embeddingProvider.embed(List.of("warranty"))).thenReturn(List.of(vector));
        UUID chunkId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(knowledgeChunkVectorRepository.findSimilar(
                        eq(tenantId), eq(knowledgeBaseId), eq(vector), anyInt(), eq(0.45)))
                .thenReturn(List.of(new KnowledgeChunkVectorRepository.RetrievedRow(
                        chunkId, documentId, 0, "3 year warranty", 0.1)));

        List<RetrievedKnowledgeChunkDto> result = service.retrieve(knowledgeBaseId, "warranty", 2);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("3 year warranty");
        assertThat(result.get(0).getDistance()).isEqualTo(0.1);
        assertThat(result.get(0).getScore()).isNotNull();
    }
}
