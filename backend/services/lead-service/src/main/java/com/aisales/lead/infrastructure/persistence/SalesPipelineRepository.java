package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.SalesPipeline;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesPipelineRepository extends JpaRepository<SalesPipeline, UUID> {

    Optional<SalesPipeline> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<SalesPipeline> findByTenantIdAndCode(UUID tenantId, String code);

    Optional<SalesPipeline> findByTenantIdAndDefaultPipelineTrueAndActiveTrue(UUID tenantId);

    List<SalesPipeline> findByTenantIdAndActiveTrueOrderByNameAsc(UUID tenantId);
}
