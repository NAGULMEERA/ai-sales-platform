package com.aisales.ai.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SemanticCacheVectorRepository {

    private final EntityManager entityManager;

    public void updateEmbedding(UUID id, float[] embedding) {
        entityManager.createNativeQuery(
                        "UPDATE semantic_cache SET query_embedding = cast(:embedding as vector) WHERE id = :id")
                .setParameter("embedding", toVectorLiteral(embedding))
                .setParameter("id", id)
                .executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public Optional<UUID> findMostSimilarId(UUID tenantId, String model, float[] embedding, double maxCosineDistance) {
        List<Object> rows = entityManager.createNativeQuery("""
                        SELECT id FROM semantic_cache
                        WHERE tenant_id = :tenantId
                          AND model_used = :model
                          AND query_embedding IS NOT NULL
                          AND (expires_at IS NULL OR expires_at > NOW())
                          AND (query_embedding <=> cast(:embedding as vector)) <= :maxDistance
                        ORDER BY query_embedding <=> cast(:embedding as vector)
                        LIMIT 1
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("model", model)
                .setParameter("embedding", toVectorLiteral(embedding))
                .setParameter("maxDistance", maxCosineDistance)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object value = rows.get(0);
        UUID id = value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
        return Optional.of(id);
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
}
