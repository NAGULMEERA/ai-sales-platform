package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.SalesPipelineTransition;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesPipelineTransitionRepository extends JpaRepository<SalesPipelineTransition, UUID> {

    List<SalesPipelineTransition> findByPipelineId(UUID pipelineId);

    List<SalesPipelineTransition> findByPipelineIdAndFromStage(UUID pipelineId, String fromStage);
}
