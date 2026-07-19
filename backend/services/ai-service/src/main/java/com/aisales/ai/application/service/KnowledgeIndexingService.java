package com.aisales.ai.application.service;

import com.aisales.ai.application.mapper.AiMapper;
import com.aisales.ai.application.rag.TextChunker;
import com.aisales.ai.domain.embedding.EmbeddingBatchResult;
import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.domain.entity.KnowledgeChunk;
import com.aisales.ai.domain.entity.KnowledgeDocument;
import com.aisales.ai.infrastructure.embedding.EmbeddingProviderRegistry;
import com.aisales.ai.infrastructure.persistence.KnowledgeChunkRepository;
import com.aisales.ai.infrastructure.persistence.KnowledgeChunkVectorRepository;
import com.aisales.ai.infrastructure.persistence.KnowledgeDocumentRepository;
import com.aisales.common.contracts.ai.IndexKnowledgeDocumentRequest;
import com.aisales.common.contracts.ai.KnowledgeDocumentDto;
import com.aisales.common.contracts.ai.KnowledgeDocumentStatus;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Track B indexing companion: plain text → chunks → embeddings → READY.
 * Embedding HTTP runs outside the persistence transaction.
 */
@Service
@RequiredArgsConstructor
public class KnowledgeIndexingService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeChunkVectorRepository knowledgeChunkVectorRepository;
    private final EmbeddingProviderRegistry embeddingProviderRegistry;
    private final TextChunker textChunker;
    private final AiMapper mapper;
    private final PlatformTransactionManager transactionManager;
    private final AiQuotaService aiQuotaService;
    private final TokenUsageService tokenUsageService;

    public KnowledgeDocumentDto indexDocument(UUID documentId, IndexKnowledgeDocumentRequest request) {
        UUID tenantId = requireTenantId();
        if (!StringUtils.hasText(request.getText())) {
            throw new ValidationException("text is required for indexing");
        }
        KnowledgeDocument document = knowledgeDocumentRepository
                .findByTenantIdAndIdAndDeletedAtIsNull(tenantId, documentId)
                .orElseThrow(() -> new NotFoundException("Knowledge document not found: " + documentId));

        List<String> chunks = textChunker.chunk(
                request.getText(), request.getChunkSizeChars(), request.getChunkOverlapChars());
        if (chunks.isEmpty()) {
            throw new ValidationException("No chunks produced from text");
        }

        EmbeddingProvider embeddingProvider = embeddingProviderRegistry.resolveDefault();
        List<float[]> vectors;
        Integer providerTokens;
        long reserved = aiQuotaService.reserveEmbed(tenantId);
        try {
            try {
                EmbeddingBatchResult batch = embeddingProvider.embedWithUsage(chunks);
                vectors = batch.vectors();
                providerTokens = batch.promptTokens();
            } catch (RuntimeException ex) {
                markFailed(tenantId, documentId);
                throw ex;
            }
            if (vectors.size() != chunks.size()) {
                markFailed(tenantId, documentId);
                throw new ValidationException("Embedding provider returned unexpected vector count");
            }

            UUID knowledgeBaseId = document.getKnowledgeBaseId();
            String modelName = embeddingProvider.modelName();
            UUID updatedBy = parseUuidOrNull(TenantContext.getUserId());
            Integer tokensForLedger = providerTokens;

            return new TransactionTemplate(transactionManager).execute(status -> {
                KnowledgeDocument managed = knowledgeDocumentRepository
                        .findByTenantIdAndIdAndDeletedAtIsNull(tenantId, documentId)
                        .orElseThrow(() -> new NotFoundException("Knowledge document not found: " + documentId));

                knowledgeChunkRepository.deleteByTenantIdAndDocumentId(tenantId, documentId);
                knowledgeChunkRepository.flush();

                Instant now = Instant.now();
                List<KnowledgeChunk> savedChunks = new ArrayList<>(chunks.size());
                for (int i = 0; i < chunks.size(); i++) {
                    savedChunks.add(knowledgeChunkRepository.save(KnowledgeChunk.builder()
                            .tenantId(tenantId)
                            .knowledgeBaseId(knowledgeBaseId)
                            .documentId(managed.getId())
                            .chunkIndex(i)
                            .content(chunks.get(i))
                            .tokenEstimate(Math.max(1, chunks.get(i).length() / 4))
                            .embeddingModel(modelName)
                            .createdAt(now)
                            .build()));
                }
                knowledgeChunkRepository.flush();

                for (int i = 0; i < savedChunks.size(); i++) {
                    knowledgeChunkVectorRepository.updateEmbedding(savedChunks.get(i).getId(), vectors.get(i));
                }

                // Charge tokens only when READY persistence succeeds (same TX).
                tokenUsageService.recordEmbeddingUsage(
                        tenantId,
                        embeddingProvider.name().toLowerCase(),
                        modelName,
                        chunks,
                        documentId.toString(),
                        "RAG_INDEX",
                        tokensForLedger);

                managed.setStatus(KnowledgeDocumentStatus.READY);
                managed.setUpdatedAt(now);
                managed.setUpdatedBy(updatedBy);
                return mapper.toDto(knowledgeDocumentRepository.save(managed));
            });
        } finally {
            aiQuotaService.release(tenantId, AiQuotaService.OPERATION_EMBED, reserved);
        }
    }

    private void markFailed(UUID tenantId, UUID documentId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            knowledgeDocumentRepository
                    .findByTenantIdAndIdAndDeletedAtIsNull(tenantId, documentId)
                    .ifPresent(doc -> {
                        // Quarantine prior chunks so FAILED docs are never searchable.
                        knowledgeChunkRepository.deleteByTenantIdAndDocumentId(tenantId, documentId);
                        knowledgeChunkRepository.flush();
                        doc.setStatus(KnowledgeDocumentStatus.FAILED);
                        doc.setUpdatedAt(Instant.now());
                        knowledgeDocumentRepository.save(doc);
                    });
        });
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
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
