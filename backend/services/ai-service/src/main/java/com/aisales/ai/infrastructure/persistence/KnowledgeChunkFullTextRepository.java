package com.aisales.ai.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * PostgreSQL full-text search over knowledge chunks (tenant + READY-document scoped).
 */
@Repository
@RequiredArgsConstructor
public class KnowledgeChunkFullTextRepository {

    private final EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<KnowledgeChunkVectorRepository.RetrievedRow> search(
            UUID tenantId, UUID knowledgeBaseId, String query, int topK) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT c.id, c.document_id, c.chunk_index, c.content,
                               ts_rank_cd(c.content_tsv, plainto_tsquery('english', :query)) AS rank
                        FROM knowledge_chunk c
                        INNER JOIN knowledge_document d ON d.id = c.document_id
                        WHERE c.tenant_id = :tenantId
                          AND c.knowledge_base_id = :knowledgeBaseId
                          AND d.tenant_id = :tenantId
                          AND d.status = 'READY'
                          AND d.deleted_at IS NULL
                          AND c.content_tsv @@ plainto_tsquery('english', :query)
                        ORDER BY rank DESC
                        LIMIT :topK
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("knowledgeBaseId", knowledgeBaseId)
                .setParameter("query", query.trim())
                .setParameter("topK", topK)
                .getResultList();

        List<KnowledgeChunkVectorRepository.RetrievedRow> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Double rank = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
            // Map rank into a distance-like score (lower is better) for DTO reuse: 1 - min(rank,1)
            double distance = Math.max(0.0, 1.0 - Math.min(1.0, rank));
            result.add(new KnowledgeChunkVectorRepository.RetrievedRow(
                    toUuid(row[0]),
                    toUuid(row[1]),
                    ((Number) row[2]).intValue(),
                    row[3] != null ? row[3].toString() : "",
                    distance));
        }
        return result;
    }

    private static UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }
}
