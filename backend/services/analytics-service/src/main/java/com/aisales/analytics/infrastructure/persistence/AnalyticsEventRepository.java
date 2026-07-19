package com.aisales.analytics.infrastructure.persistence;

import com.aisales.analytics.domain.entity.AnalyticsEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    @Query("""
            SELECT COUNT(e) FROM AnalyticsEvent e
            WHERE e.tenantId = :tenantId
              AND e.metricName = :metricName
              AND e.occurredAt >= :from
              AND e.occurredAt < :to
              AND e.deletedAt IS NULL
            """)
    long countMetric(
            @Param("tenantId") UUID tenantId,
            @Param("metricName") String metricName,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query(value = """
            SELECT COALESCE(AVG(e.metric_value), 0)
            FROM analytics_event e
            WHERE e.tenant_id = :tenantId
              AND e.metric_name = :metricName
              AND e.occurred_at >= :from
              AND e.occurred_at < :to
              AND e.deleted_at IS NULL
            """, nativeQuery = true)
    Double avgMetric(
            @Param("tenantId") UUID tenantId,
            @Param("metricName") String metricName,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query(value = """
            SELECT CAST(e.occurred_at AS date) AS metric_date, COUNT(*) AS cnt
            FROM analytics_event e
            WHERE e.tenant_id = :tenantId
              AND e.metric_name = :metricName
              AND e.occurred_at >= :from
              AND e.occurred_at < :to
              AND e.deleted_at IS NULL
            GROUP BY CAST(e.occurred_at AS date)
            ORDER BY metric_date
            """, nativeQuery = true)
    List<Object[]> dailySeries(
            @Param("tenantId") UUID tenantId,
            @Param("metricName") String metricName,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query(value = """
            SELECT COALESCE(e.dimensions ->> :dimKey, 'UNKNOWN') AS dim_value, COUNT(*) AS cnt
            FROM analytics_event e
            WHERE e.tenant_id = :tenantId
              AND e.metric_name = :metricName
              AND e.occurred_at >= :from
              AND e.occurred_at < :to
              AND e.deleted_at IS NULL
            GROUP BY dim_value
            ORDER BY cnt DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> topDimensions(
            @Param("tenantId") UUID tenantId,
            @Param("metricName") String metricName,
            @Param("dimKey") String dimKey,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("limit") int limit);

    @Query(value = """
            SELECT COALESCE(e.dimensions ->> 'status', 'UNKNOWN') AS status, COUNT(*) AS cnt
            FROM analytics_event e
            WHERE e.tenant_id = :tenantId
              AND e.metric_name = :metricName
              AND e.occurred_at >= :from
              AND e.occurred_at < :to
              AND e.deleted_at IS NULL
            GROUP BY status
            ORDER BY cnt DESC
            """, nativeQuery = true)
    List<Object[]> countByStatus(
            @Param("tenantId") UUID tenantId,
            @Param("metricName") String metricName,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
