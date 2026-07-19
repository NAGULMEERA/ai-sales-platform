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
     * Batch-write embeddings for already-persisted chunks (same semantics as
     * {@link #updateEmbedding(UUID, float[])}, fewer round-trips).
     */
    public void updateEmbeddings(List<UUID> chunkIds, List<float[]> embeddings) {
        if (chunkIds == null || embeddings == null || chunkIds.isEmpty()) {
            return;
        }
        if (chunkIds.size() != embeddings.size()) {
            throw new IllegalArgumentException("chunkIds and embeddings size mismatch");
        }
        for (int i = 0; i < chunkIds.size(); i++) {
            updateEmbedding(chunkIds.get(i), embeddings.get(i));
        }
        entityManager.flush();
    }

    /**
     * Top-k cosine nearest chunks for a knowledge base (tenant isolated).
     * Only chunks belonging to READY documents are searchable.
     */
    @SuppressWarnings("unchecked")
    public List<RetrievedRow> findSimilar(
            UUID tenantId, UUID knowledgeBaseId, float[] embedding, int topK, double maxCosineDistance) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT c.id, c.document_id, c.chunk_index, c.content,
                               (c.embedding <=> cast(:embedding as vector)) AS distance
                        FROM knowledge_chunk c
                        INNER JOIN knowledge_document d ON d.id = c.document_id
                        WHERE c.tenant_id = :tenantId
                          AND c.knowledge_base_id = :knowledgeBaseId
                          AND d.tenant_id = :tenantId
                          AND d.status = 'READY'
                          AND d.deleted_at IS NULL
                          AND c.embedding IS NOT NULL
                          AND (c.embedding <=> cast(:embedding as vector)) <= :maxDistance
                        ORDER BY c.embedding <=> cast(:embedding as vector)
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
