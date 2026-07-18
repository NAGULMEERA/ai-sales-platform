package com.aisales.workflow.infrastructure.persistence;

import com.aisales.workflow.domain.entity.WorkflowExecution;
import com.aisales.workflow.domain.enums.WorkflowDefinitionKey;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {

    Optional<WorkflowExecution> findByTenantIdAndDefinitionKeyAndBusinessKey(
            UUID tenantId, WorkflowDefinitionKey definitionKey, String businessKey);
}
