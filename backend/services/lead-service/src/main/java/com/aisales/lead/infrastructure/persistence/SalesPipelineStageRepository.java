package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.SalesPipelineStage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesPipelineStageRepository extends JpaRepository<SalesPipelineStage, UUID> {

    List<SalesPipelineStage> findByPipelineIdOrderByStageOrderAsc(UUID pipelineId);

    List<SalesPipelineStage> findByPipelineIdInOrderByStageOrderAsc(Collection<UUID> pipelineIds);
}
