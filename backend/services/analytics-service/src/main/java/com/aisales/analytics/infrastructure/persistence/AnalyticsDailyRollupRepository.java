package com.aisales.analytics.infrastructure.persistence;

import com.aisales.analytics.domain.entity.AnalyticsDailyRollup;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsDailyRollupRepository extends JpaRepository<AnalyticsDailyRollup, UUID> {

    Optional<AnalyticsDailyRollup> findByTenantIdAndMetricDateAndMetricName(
            UUID tenantId, LocalDate metricDate, String metricName);
}
