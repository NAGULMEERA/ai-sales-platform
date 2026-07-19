package com.aisales.ai.application.rag;

import com.aisales.ai.application.mapper.AiMapper;
import com.aisales.ai.application.service.AiQuotaService;
import com.aisales.ai.application.service.TokenUsageService;
import com.aisales.ai.domain.embedding.EmbeddingBatchResult;
import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.domain.entity.KnowledgeChunk;
import com.aisales.ai.domain.entity.KnowledgeDocument;
import com.aisales.ai.infrastructure.embedding.EmbeddingProviderRegistry;
import com.aisales.ai.infrastructure.persistence.KnowledgeChunkRepository;
import com.aisales.ai.infrastructure.persistence.KnowledgeChunkVectorRepository;
import com.aisales.ai.infrastructure.persistence.KnowledgeDocumentRepository;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Document → Chunk → Embed → Persist pipeline. Embedding HTTP runs outside the DB transaction.
 */
@Component
@RequiredArgsConstructor
public class DocumentEmbeddingPipeline {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeChunkVectorRepository knowledgeChunkVectorRepository;
    private final EmbeddingProviderRegistry embeddingProviderRegistry;
    private final TextChunker textChunker;
    private final AiMapper mapper;
    private final PlatformTransactionManager transactionManager;
    private final AiQuotaService aiQuotaService;
    private final TokenUsageService tokenUsageService;

    public KnowledgeDocumentDto index(
            UUID tenantId,
            KnowledgeDocument document,
            String text,
            Integer chunkSizeChars,
            Integer chunkOverlapChars) {
        List<String> chunks = textChunker.chunk(text, chunkSizeChars, chunkOverlapChars);
        if (chunks.isEmpty()) {
            throw new ValidationException("No chunks produced from text");
        }

        UUID documentId = document.getId();
        claimForIndexing(tenantId, documentId);

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
                markFailedIfClaimHeld(tenantId, documentId);
                throw ex;
            }
            if (vectors.size() != chunks.size()) {
                markFailedIfClaimHeld(tenantId, documentId);
                throw new ValidationException("Embedding provider returned unexpected vector count");
            }

            UUID knowledgeBaseId = document.getKnowledgeBaseId();
            String modelName = embeddingProvider.modelName();
            UUID updatedBy = parseUuidOrNull(TenantContext.getUserId());
            Integer tokensForLedger = providerTokens;

            return new TransactionTemplate(transactionManager).execute(status -> {
                KnowledgeDocument managed = knowledgeDocumentRepository
                        .findByTenantIdAndIdForUpdate(tenantId, documentId)
                        .orElseThrow(() -> new NotFoundException("Knowledge document not found: " + documentId));
                if (managed.getStatus() != KnowledgeDocumentStatus.INDEXING) {
                    throw new ValidationException(
                            "Document indexing claim lost; status=" + managed.getStatus());
                }

                knowledgeChunkRepository.deleteByTenantIdAndDocumentId(tenantId, documentId);
                knowledgeChunkRepository.flush();

                Instant now = Instant.now();
                List<KnowledgeChunk> toSave = new ArrayList<>(chunks.size());
                for (int i = 0; i < chunks.size(); i++) {
                    toSave.add(KnowledgeChunk.builder()
                            .tenantId(tenantId)
                            .knowledgeBaseId(knowledgeBaseId)
                            .documentId(managed.getId())
                            .chunkIndex(i)
                            .content(chunks.get(i))
                            .tokenEstimate(Math.max(1, chunks.get(i).length() / 4))
                            .embeddingModel(modelName)
                            .createdAt(now)
                            .build());
                }
                List<KnowledgeChunk> savedChunks = knowledgeChunkRepository.saveAll(toSave);
                knowledgeChunkRepository.flush();

                List<UUID> chunkIds = savedChunks.stream().map(KnowledgeChunk::getId).toList();
                knowledgeChunkVectorRepository.updateEmbeddings(chunkIds, vectors);

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

    private void claimForIndexing(UUID tenantId, UUID documentId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            KnowledgeDocument doc = knowledgeDocumentRepository
                    .findByTenantIdAndIdForUpdate(tenantId, documentId)
                    .orElseThrow(() -> new NotFoundException("Knowledge document not found: " + documentId));
            if (doc.getStatus() == KnowledgeDocumentStatus.INDEXING) {
                throw new ValidationException("Document is already being indexed: " + documentId);
            }
            if (doc.getStatus() == KnowledgeDocumentStatus.ARCHIVED) {
                throw new ValidationException("Archived documents cannot be indexed: " + documentId);
            }
            doc.setStatus(KnowledgeDocumentStatus.INDEXING);
            doc.setUpdatedAt(Instant.now());
            knowledgeDocumentRepository.save(doc);
        });
    }

    /**
     * Quarantine only when this indexer still holds the INDEXING claim. Never wipe a
     * peer's READY result.
     */
    private void markFailedIfClaimHeld(UUID tenantId, UUID documentId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            knowledgeDocumentRepository
                    .findByTenantIdAndIdForUpdate(tenantId, documentId)
                    .ifPresent(doc -> {
                        if (doc.getStatus() != KnowledgeDocumentStatus.INDEXING) {
                            return;
                        }
                        knowledgeChunkRepository.deleteByTenantIdAndDocumentId(tenantId, documentId);
                        knowledgeChunkRepository.flush();
                        doc.setStatus(KnowledgeDocumentStatus.FAILED);
                        doc.setUpdatedAt(Instant.now());
                        knowledgeDocumentRepository.save(doc);
                    });
        });
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
