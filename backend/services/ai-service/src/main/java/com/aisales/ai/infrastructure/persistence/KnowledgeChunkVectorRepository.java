package com.aisales.ai.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KnowledgeChunkVectorRepository {

    private final EntityManager entityManager;

    public void updateEmbedding(UUID chunkId, float[] embedding) {
        entityManager.createNativeQuery(
                        "UPDATE knowledge_chunk SET embedding = cast(:embedding as vector) WHERE id = :id")
                .setParameter("embedding", toVectorLiteral(embedding))
                .setParameter("id", chunkId)
                .executeUpdate();
    }

    /**
     * Top-k cosine nearest chunks for a knowledge base (tenant isolated).
     */
    @SuppressWarnings("unchecked")
    public List<RetrievedRow> findSimilar(
            UUID tenantId, UUID knowledgeBaseId, float[] embedding, int topK, double maxCosineDistance) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT id, document_id, chunk_index, content,
                               (embedding <=> cast(:embedding as vector)) AS distance
                        FROM knowledge_chunk
                        WHERE tenant_id = :tenantId
                          AND knowledge_base_id = :knowledgeBaseId
                          AND embedding IS NOT NULL
                          AND (embedding <=> cast(:embedding as vector)) <= :maxDistance
                        ORDER BY embedding <=> cast(:embedding as vector)
                        LIMIT :topK
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("knowledgeBaseId", knowledgeBaseId)
                .setParameter("embedding", toVectorLiteral(embedding))
                .setParameter("maxDistance", maxCosineDistance)
                .setParameter("topK", topK)
                .getResultList();

        List<RetrievedRow> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new RetrievedRow(
                    toUuid(row[0]),
                    toUuid(row[1]),
                    ((Number) row[2]).intValue(),
                    row[3] != null ? row[3].toString() : "",
                    row[4] != null ? ((Number) row[4]).doubleValue() : null));
        }
        return result;
    }

    private static UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }

    private static String toVectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(embedding[i]);
        }
        return builder.append(']').toString();
    }

    public record RetrievedRow(
            UUID chunkId, UUID documentId, int chunkIndex, String content, Double distance) {
    }
}
