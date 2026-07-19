package com.aisales.ai.infrastructure.persistence;

import com.aisales.ai.domain.entity.TokenUsage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenUsageRepository extends JpaRepository<TokenUsage, UUID> {

    @Query("""
            SELECT COALESCE(SUM(t.totalTokens), 0)
            FROM TokenUsage t
            WHERE t.tenantId = :tenantId
              AND t.createdAt >= :fromInclusive
            """)
    long sumTotalTokensSince(
            @Param("tenantId") UUID tenantId, @Param("fromInclusive") Instant fromInclusive);

    @Query("""
            SELECT COALESCE(SUM(t.totalTokens), 0)
            FROM TokenUsage t
            WHERE t.tenantId = :tenantId
              AND t.operation = :operation
              AND t.createdAt >= :fromInclusive
            """)
    long sumTotalTokensSinceByOperation(
            @Param("tenantId") UUID tenantId,
            @Param("operation") String operation,
            @Param("fromInclusive") Instant fromInclusive);

    /**
     * Rows: operation, provider, model, promptTokens, completionTokens, totalTokens,
     * estimatedCostUsd, requestCount.
     */
    @Query("""
            SELECT t.operation,
                   t.provider,
                   t.model,
                   COALESCE(SUM(t.promptTokens), 0),
                   COALESCE(SUM(t.completionTokens), 0),
                   COALESCE(SUM(t.totalTokens), 0),
                   COALESCE(SUM(t.estimatedCostUsd), 0),
                   COUNT(t)
            FROM TokenUsage t
            WHERE t.tenantId = :tenantId
              AND t.createdAt >= :fromInclusive
              AND t.createdAt < :toExclusive
            GROUP BY t.operation, t.provider, t.model
            ORDER BY t.operation, t.provider, t.model
            """)
    List<Object[]> aggregateUsageByModel(
            @Param("tenantId") UUID tenantId,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive);
}
