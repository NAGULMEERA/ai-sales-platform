package com.aisales.ai.application.rag;

import com.aisales.ai.infrastructure.configuration.RagProperties;
import com.aisales.ai.infrastructure.persistence.KnowledgeBaseRepository;
import com.aisales.ai.infrastructure.persistence.KnowledgeChunkFullTextRepository;
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
 * Lexical full-text retrieval over knowledge chunks (PostgreSQL tsvector).
 */
@Component
@RequiredArgsConstructor
public class KeywordRetriever implements Retriever {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeChunkFullTextRepository fullTextRepository;
    private final PlatformTransactionManager transactionManager;
    private final RerankerRegistry rerankerRegistry;
    private final RagProperties ragProperties;

    @Override
    public String name() {
        return "KEYWORD";
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
        int candidateLimit = Math.min(limit * 3, 40);

        List<KnowledgeChunkVectorRepository.RetrievedRow> rows =
                new TransactionTemplate(transactionManager).execute(status ->
                        fullTextRepository.search(tenantId, knowledgeBaseId, query.trim(), candidateLimit));

        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<RetrievedKnowledgeChunkDto> candidates = rows.stream()
                .map(row -> RetrievedKnowledgeChunkDto.builder()
                        .chunkId(row.chunkId())
                        .documentId(row.documentId())
                        .chunkIndex(row.chunkIndex())
                        .content(row.content())
                        .distance(row.distance())
                        .build())
                .toList();
        return rerankerRegistry.resolveDefault().rerank(query.trim(), candidates, limit);
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }
}
