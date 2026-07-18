package com.aisales.ai.application.service;

import com.aisales.ai.application.rag.RerankerRegistry;
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
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Tenant-scoped RAG retrieval. Embedding HTTP and rerank run outside the DB transaction.
 */
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeChunkVectorRepository knowledgeChunkVectorRepository;
    private final EmbeddingProviderRegistry embeddingProviderRegistry;
    private final PlatformTransactionManager transactionManager;
    private final TokenUsageService tokenUsageService;
    private final RerankerRegistry rerankerRegistry;
    private final RagProperties ragProperties;

    public List<RetrievedKnowledgeChunkDto> retrieve(
            UUID knowledgeBaseId, String query, Integer topK) {
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
        List<float[]> vectors = provider.embed(List.of(trimmedQuery));
        if (vectors.isEmpty()) {
            return List.of();
        }

        tokenUsageService.recordEmbeddingUsage(
                tenantId,
                provider.name().toLowerCase(),
                provider.modelName(),
                List.of(trimmedQuery),
                knowledgeBaseId.toString(),
                "RAG_RETRIEVE");

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

    public String resolveQuery(String retrievalQuery, Map<String, String> variables) {
        if (StringUtils.hasText(retrievalQuery)) {
            return retrievalQuery.trim();
        }
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        if (StringUtils.hasText(variables.get("question"))) {
            return variables.get("question").trim();
        }
        if (StringUtils.hasText(variables.get("query"))) {
            return variables.get("query").trim();
        }
        return String.join(" ", variables.values()).trim();
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
