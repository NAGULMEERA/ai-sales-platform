package com.aisales.ai.application.service;

import com.aisales.ai.application.rag.DocumentExtractor;
import com.aisales.ai.application.rag.DocumentExtractorRegistry;
import com.aisales.ai.domain.entity.KnowledgeDocument;
import com.aisales.ai.infrastructure.media.MediaContentClient;
import com.aisales.ai.infrastructure.persistence.KnowledgeDocumentRepository;
import com.aisales.common.contracts.ai.IndexKnowledgeDocumentRequest;
import com.aisales.common.contracts.ai.IngestKnowledgeDocumentRequest;
import com.aisales.common.contracts.ai.KnowledgeDocumentDto;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Media → extract → existing RAG index path.
 * Binary stays in Media Service; AI Service only receives bytes for extraction.
 */
@Service
@RequiredArgsConstructor
public class KnowledgeIngestService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final MediaContentClient mediaContentClient;
    private final DocumentExtractorRegistry documentExtractorRegistry;
    private final KnowledgeIndexingService knowledgeIndexingService;

    public KnowledgeDocumentDto ingest(UUID documentId, IngestKnowledgeDocumentRequest request) {
        UUID tenantId = requireTenantId();
        KnowledgeDocument document = knowledgeDocumentRepository
                .findByTenantIdAndIdAndDeletedAtIsNull(tenantId, documentId)
                .orElseThrow(() -> new NotFoundException("Knowledge document not found: " + documentId));
        if (document.getMediaId() == null) {
            throw new ValidationException(
                    "Knowledge document has no mediaId; upload to Media Service and register mediaId first");
        }

        MediaContentClient.MediaBinary binary = mediaContentClient.download(document.getMediaId());
        String contentType = StringUtils.hasText(document.getContentType())
                ? document.getContentType()
                : binary.contentType();
        String filename = StringUtils.hasText(document.getName()) ? document.getName() : binary.filename();

        String extractorOverride = request != null ? request.getExtractor() : null;
        DocumentExtractor extractor =
                documentExtractorRegistry.resolve(extractorOverride, contentType, filename);
        String text = extractor.extract(binary.content(), contentType, filename);
        if (!StringUtils.hasText(text)) {
            throw new ValidationException("Extractor " + extractor.name() + " produced empty text");
        }

        IndexKnowledgeDocumentRequest indexRequest = IndexKnowledgeDocumentRequest.builder()
                .text(text)
                .chunkSizeChars(request != null ? request.getChunkSizeChars() : null)
                .chunkOverlapChars(request != null ? request.getChunkOverlapChars() : null)
                .build();
        return knowledgeIndexingService.indexDocument(documentId, indexRequest);
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }
}
