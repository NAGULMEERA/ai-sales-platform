package com.aisales.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.ai.application.mapper.AiMapper;
import com.aisales.ai.application.rag.CharWindowChunker;
import com.aisales.ai.application.rag.TextChunker;
import com.aisales.ai.application.rag.TokenWindowChunker;
import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.domain.entity.KnowledgeChunk;
import com.aisales.ai.infrastructure.configuration.RagProperties;
import com.aisales.ai.domain.entity.KnowledgeDocument;
import com.aisales.ai.infrastructure.embedding.EmbeddingProviderRegistry;
import com.aisales.ai.infrastructure.persistence.KnowledgeChunkRepository;
import com.aisales.ai.infrastructure.persistence.KnowledgeChunkVectorRepository;
import com.aisales.ai.infrastructure.persistence.KnowledgeDocumentRepository;
import com.aisales.common.contracts.ai.IndexKnowledgeDocumentRequest;
import com.aisales.common.contracts.ai.KnowledgeDocumentDto;
import com.aisales.common.contracts.ai.KnowledgeDocumentStatus;
import com.aisales.common.core.util.TenantContext;
import java.time.Instant;
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

@ExtendWith(MockitoExtension.class)
class KnowledgeIndexingServiceTest {

    @Mock private KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Mock private KnowledgeChunkRepository knowledgeChunkRepository;
    @Mock private KnowledgeChunkVectorRepository knowledgeChunkVectorRepository;
    @Mock private EmbeddingProviderRegistry embeddingProviderRegistry;
    @Mock private EmbeddingProvider embeddingProvider;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private AiQuotaService aiQuotaService;
    @Mock private TokenUsageService tokenUsageService;

    private KnowledgeIndexingService service;
    private UUID tenantId;
    private UUID documentId;
    private UUID knowledgeBaseId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        knowledgeBaseId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        RagProperties ragProperties = new RagProperties();
        ragProperties.setChunker("TOKEN");
        TextChunker textChunker = new TextChunker(
                List.of(new CharWindowChunker(ragProperties), new TokenWindowChunker(ragProperties)),
                ragProperties);
        service = new KnowledgeIndexingService(
                knowledgeDocumentRepository,
                knowledgeChunkRepository,
                knowledgeChunkVectorRepository,
                embeddingProviderRegistry,
                textChunker,
                new AiMapper(),
                transactionManager,
                aiQuotaService,
                tokenUsageService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldIndexTextAndMarkDocumentReady() {
        KnowledgeDocument document = KnowledgeDocument.builder()
                .id(documentId)
                .tenantId(tenantId)
                .knowledgeBaseId(knowledgeBaseId)
                .name("faq.txt")
                .status(KnowledgeDocumentStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(knowledgeDocumentRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, documentId))
                .thenReturn(Optional.of(document));
        when(embeddingProviderRegistry.resolveDefault()).thenReturn(embeddingProvider);
        when(embeddingProvider.name()).thenReturn("STUB");
        when(embeddingProvider.modelName()).thenReturn("stub-embedding-1024");
        when(embeddingProvider.embed(anyList())).thenReturn(List.of(new float[1024]));
        when(knowledgeChunkRepository.save(any(KnowledgeChunk.class))).thenAnswer(inv -> {
            KnowledgeChunk chunk = inv.getArgument(0);
            chunk.setId(UUID.randomUUID());
            return chunk;
        });
        when(knowledgeDocumentRepository.save(any(KnowledgeDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        KnowledgeDocumentDto dto = service.indexDocument(documentId, IndexKnowledgeDocumentRequest.builder()
                .text("Warranty covers three years.")
                .build());

        assertThat(dto.getStatus()).isEqualTo(KnowledgeDocumentStatus.READY);
        verify(knowledgeChunkRepository).deleteByTenantIdAndDocumentId(tenantId, documentId);
        verify(knowledgeChunkVectorRepository).updateEmbedding(any(UUID.class), any(float[].class));
        verify(embeddingProvider).embed(eq(List.of("Warranty covers three years.")));
    }
}
