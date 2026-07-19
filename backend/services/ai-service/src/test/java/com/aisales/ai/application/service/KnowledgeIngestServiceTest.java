package com.aisales.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.ai.application.rag.DocumentExtractorRegistry;
import com.aisales.ai.application.rag.TextDocumentExtractor;
import com.aisales.ai.domain.entity.KnowledgeDocument;
import com.aisales.ai.infrastructure.configuration.RagProperties;
import com.aisales.ai.infrastructure.media.MediaContentClient;
import com.aisales.ai.infrastructure.persistence.KnowledgeDocumentRepository;
import com.aisales.common.contracts.ai.IndexKnowledgeDocumentRequest;
import com.aisales.common.contracts.ai.IngestKnowledgeDocumentRequest;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeIngestServiceTest {

    @Mock private KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Mock private MediaContentClient mediaContentClient;
    @Mock private KnowledgeIndexingService knowledgeIndexingService;

    private KnowledgeIngestService service;
    private UUID tenantId;
    private UUID documentId;
    private UUID mediaId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        mediaId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        RagProperties ragProperties = new RagProperties();
        ragProperties.setExtractor("AUTO");
        DocumentExtractorRegistry registry =
                new DocumentExtractorRegistry(List.of(new TextDocumentExtractor()), ragProperties);
        service = new KnowledgeIngestService(
                knowledgeDocumentRepository, mediaContentClient, registry, knowledgeIndexingService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldDownloadExtractAndIndex() {
        when(knowledgeDocumentRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, documentId))
                .thenReturn(Optional.of(KnowledgeDocument.builder()
                        .id(documentId)
                        .tenantId(tenantId)
                        .knowledgeBaseId(UUID.randomUUID())
                        .name("faq.txt")
                        .contentType("text/plain")
                        .mediaId(mediaId)
                        .status(KnowledgeDocumentStatus.PENDING)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build()));
        when(mediaContentClient.download(mediaId))
                .thenReturn(new MediaContentClient.MediaBinary(
                        "Warranty covers three years.".getBytes(), "text/plain", "faq.txt"));
        when(knowledgeIndexingService.indexDocument(eq(documentId), any()))
                .thenReturn(KnowledgeDocumentDto.builder()
                        .id(documentId)
                        .status(KnowledgeDocumentStatus.READY)
                        .build());

        KnowledgeDocumentDto dto = service.ingest(documentId, IngestKnowledgeDocumentRequest.builder().build());

        assertThat(dto.getStatus()).isEqualTo(KnowledgeDocumentStatus.READY);
        ArgumentCaptor<IndexKnowledgeDocumentRequest> captor =
                ArgumentCaptor.forClass(IndexKnowledgeDocumentRequest.class);
        verify(knowledgeIndexingService).indexDocument(eq(documentId), captor.capture());
        assertThat(captor.getValue().getText()).contains("Warranty");
    }
}
