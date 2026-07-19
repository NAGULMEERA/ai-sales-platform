package com.aisales.workflow.infrastructure.persistence;

import com.aisales.workflow.domain.entity.WorkflowAutomationExecution;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowAutomationExecutionRepository
        extends JpaRepository<WorkflowAutomationExecution, UUID> {

    Optional<WorkflowAutomationExecution> findByTenantIdAndId(UUID tenantId, UUID id);

    List<WorkflowAutomationExecution> findByTenantIdAndStateAndNextRetryAtLessThanEqual(
            UUID tenantId, String state, Instant nextRetryAt);
}
