package com.aisales.ai.infrastructure.persistence;

import com.aisales.ai.domain.entity.TenantAiQuota;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantAiQuotaRepository extends JpaRepository<TenantAiQuota, UUID> {

    Optional<TenantAiQuota> findByTenantId(UUID tenantId);
}
