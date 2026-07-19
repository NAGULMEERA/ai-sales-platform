package com.aisales.analytics.infrastructure.persistence;

import com.aisales.analytics.domain.entity.AnalyticsDailyRollup;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalyticsDailyRollupRepository extends JpaRepository<AnalyticsDailyRollup, UUID> {

    Optional<AnalyticsDailyRollup> findByTenantIdAndMetricDateAndMetricName(
            UUID tenantId, LocalDate metricDate, String metricName);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO analytics_daily_rollup
              (id, tenant_id, organization_id, metric_date, metric_name, metric_sum, metric_count,
               dimensions, created_at, updated_at, version)
            VALUES
              (:id, :tenantId, NULL, :metricDate, :metricName, :metricValue, 1,
               '{}'::jsonb, NOW(), NOW(), 0)
            ON CONFLICT (tenant_id, metric_date, metric_name) DO UPDATE SET
              metric_sum = analytics_daily_rollup.metric_sum + EXCLUDED.metric_sum,
              metric_count = analytics_daily_rollup.metric_count + 1,
              updated_at = NOW(),
              version = analytics_daily_rollup.version + 1
            """, nativeQuery = true)
    int upsertDailyRollup(
            @Param("id") UUID id,
            @Param("tenantId") UUID tenantId,
            @Param("metricDate") LocalDate metricDate,
            @Param("metricName") String metricName,
            @Param("metricValue") double metricValue);
}
