package com.aisales.ai.application.rag;

import com.aisales.ai.application.service.AiQuotaService;
import com.aisales.ai.application.service.TokenUsageService;
import com.aisales.ai.domain.embedding.EmbeddingBatchResult;
import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.infrastructure.configuration.RagProperties;
import com.aisales.ai.infrastructure.embedding.EmbeddingProviderRegistry;
import com.aisales.ai.infrastructure.persistence.KnowledgeBaseRepository;
import com.aisales.ai.infrastructure.persistence.KnowledgeChunkVectorRepository;
import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Dense vector retrieval over pgvector (tenant + READY-document scoped).
 */
@Component
@RequiredArgsConstructor
public class VectorRetriever implements Retriever {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeChunkVectorRepository knowledgeChunkVectorRepository;
    private final EmbeddingProviderRegistry embeddingProviderRegistry;
    private final PlatformTransactionManager transactionManager;
    private final TokenUsageService tokenUsageService;
    private final AiQuotaService aiQuotaService;
    private final RerankerRegistry rerankerRegistry;
    private final RagProperties ragProperties;

    @Override
    public String name() {
        return "VECTOR";
    }

    @Override
    public List<RetrievedKnowledgeChunkDto> retrieve(UUID knowledgeBaseId, String query, Integer topK) {
        UUID tenantId = requireTenantId();
        knowledgeBaseRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, knowledgeBaseId)
                .orElseThrow(() -> new NotFoundException("Knowledge base not found: " + knowledgeBaseId));

        if (!StringUtils.hasText(query)) {
            throw new ValidationException("retrieval query is required when knowledgeBaseId is set");
        }

        int limit = topK != null && topK > 0
                ? Math.min(topK, 20)
                : ragProperties.getDefaultTopK();
        int candidateLimit = candidateLimit(limit);

        EmbeddingProvider provider = embeddingProviderRegistry.resolveDefault();
        String trimmedQuery = query.trim();
        long reserved = aiQuotaService.reserveEmbed(tenantId);
        EmbeddingBatchResult batch;
        try {
            batch = provider.embedWithUsage(List.of(trimmedQuery));
            tokenUsageService.recordEmbeddingUsage(
                    tenantId,
                    provider.name().toLowerCase(),
                    provider.modelName(),
                    List.of(trimmedQuery),
                    knowledgeBaseId.toString(),
                    "RAG_RETRIEVE",
                    batch.promptTokens());
        } finally {
            aiQuotaService.release(tenantId, AiQuotaService.OPERATION_EMBED, reserved);
        }
        List<float[]> vectors = batch.vectors();
        if (vectors.isEmpty()) {
            return List.of();
        }

        float[] queryVector = vectors.get(0);
        double maxDistance = ragProperties.getMaxCosineDistance();
        List<RetrievedKnowledgeChunkDto> candidates = new TransactionTemplate(transactionManager).execute(status ->
                knowledgeChunkVectorRepository
                        .findSimilar(tenantId, knowledgeBaseId, queryVector, candidateLimit, maxDistance)
                        .stream()
                        .map(row -> RetrievedKnowledgeChunkDto.builder()
                                .chunkId(row.chunkId())
                                .documentId(row.documentId())
                                .chunkIndex(row.chunkIndex())
                                .content(row.content())
                                .distance(row.distance())
                                .build())
                        .toList());

        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        return rerankerRegistry.resolveDefault().rerank(trimmedQuery, candidates, limit);
    }

    private int candidateLimit(int topK) {
        RagProperties.Rerank rerank = ragProperties.getRerank();
        int multiplied = Math.max(topK, topK * Math.max(1, rerank.getCandidateMultiplier()));
        return Math.min(multiplied, Math.max(topK, rerank.getMaxCandidates()));
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }
}
