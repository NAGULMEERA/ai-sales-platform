package com.aisales.ai.application.service;

import com.aisales.ai.application.rag.DocumentEmbeddingPipeline;
import com.aisales.ai.domain.entity.KnowledgeDocument;
import com.aisales.ai.infrastructure.persistence.KnowledgeDocumentRepository;
import com.aisales.common.contracts.ai.IndexKnowledgeDocumentRequest;
import com.aisales.common.contracts.ai.KnowledgeDocumentDto;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Track B indexing facade: validates ownership then delegates to {@link DocumentEmbeddingPipeline}.
 */
@Service
@RequiredArgsConstructor
public class KnowledgeIndexingService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final DocumentEmbeddingPipeline documentEmbeddingPipeline;

    public KnowledgeDocumentDto indexDocument(UUID documentId, IndexKnowledgeDocumentRequest request) {
        UUID tenantId = requireTenantId();
        if (!StringUtils.hasText(request.getText())) {
            throw new ValidationException("text is required for indexing");
        }
        KnowledgeDocument document = knowledgeDocumentRepository
                .findByTenantIdAndIdAndDeletedAtIsNull(tenantId, documentId)
                .orElseThrow(() -> new NotFoundException("Knowledge document not found: " + documentId));

        return documentEmbeddingPipeline.index(
                tenantId,
                document,
                request.getText(),
                request.getChunkSizeChars(),
                request.getChunkOverlapChars());
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }
}
