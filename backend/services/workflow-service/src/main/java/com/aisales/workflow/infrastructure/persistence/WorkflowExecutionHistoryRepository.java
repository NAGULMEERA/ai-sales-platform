package com.aisales.workflow.infrastructure.persistence;

import com.aisales.workflow.domain.entity.WorkflowExecutionHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowExecutionHistoryRepository
        extends JpaRepository<WorkflowExecutionHistory, UUID> {

    List<WorkflowExecutionHistory> findByTenantIdAndExecutionIdOrderByOccurredAtAsc(
            UUID tenantId, UUID executionId);
}
