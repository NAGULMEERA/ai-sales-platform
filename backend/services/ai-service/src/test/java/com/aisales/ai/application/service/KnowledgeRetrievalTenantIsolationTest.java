package com.aisales.ai.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.aisales.ai.application.rag.HybridRetriever;
import com.aisales.ai.application.rag.KeywordRetriever;
import com.aisales.ai.application.rag.NoneReranker;
import com.aisales.ai.application.rag.RerankerRegistry;
import com.aisales.ai.application.rag.RetrieverRegistry;
import com.aisales.ai.application.rag.StubReranker;
import com.aisales.ai.application.rag.VectorRetriever;
import com.aisales.ai.infrastructure.configuration.RagProperties;
import com.aisales.ai.infrastructure.embedding.EmbeddingProviderRegistry;
import com.aisales.ai.infrastructure.persistence.KnowledgeBaseRepository;
import com.aisales.ai.infrastructure.persistence.KnowledgeChunkFullTextRepository;
import com.aisales.ai.infrastructure.persistence.KnowledgeChunkVectorRepository;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.NotFoundException;
import java.util.List;
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

/**
 * Tenant A must not retrieve knowledge owned by another tenant (repository scoped by TenantContext).
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeRetrievalTenantIsolationTest {

    @Mock private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock private KnowledgeChunkVectorRepository knowledgeChunkVectorRepository;
    @Mock private KnowledgeChunkFullTextRepository knowledgeChunkFullTextRepository;
    @Mock private EmbeddingProviderRegistry embeddingProviderRegistry;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private TokenUsageService tokenUsageService;
    @Mock private AiQuotaService aiQuotaService;

    private KnowledgeRetrievalService service;
    private UUID tenantA;
    private UUID foreignKnowledgeBaseId;

    @BeforeEach
    void setUp() {
        tenantA = UUID.randomUUID();
        foreignKnowledgeBaseId = UUID.randomUUID();
        TenantContext.setTenantId(tenantA.toString());
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        RagProperties ragProperties = new RagProperties();
        ragProperties.setRetriever("VECTOR");
        ragProperties.setReranker("NONE");
        RerankerRegistry rerankerRegistry =
                new RerankerRegistry(List.of(new NoneReranker(), new StubReranker()), ragProperties);
        VectorRetriever vectorRetriever = new VectorRetriever(
                knowledgeBaseRepository,
                knowledgeChunkVectorRepository,
                embeddingProviderRegistry,
                transactionManager,
                tokenUsageService,
                aiQuotaService,
                rerankerRegistry,
                ragProperties);
        KeywordRetriever keywordRetriever = new KeywordRetriever(
                knowledgeBaseRepository,
                knowledgeChunkFullTextRepository,
                transactionManager,
                rerankerRegistry,
                ragProperties);
        RetrieverRegistry retrieverRegistry = new RetrieverRegistry(
                List.of(vectorRetriever, keywordRetriever, new HybridRetriever(vectorRetriever, keywordRetriever)),
                ragProperties);
        service = new KnowledgeRetrievalService(retrieverRegistry);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldNotRetrieveKnowledgeBaseOwnedByOtherTenant() {
        when(knowledgeBaseRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantA, foreignKnowledgeBaseId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retrieve(foreignKnowledgeBaseId, "warranty", 5))
                .isInstanceOf(NotFoundException.class);
    }
}
