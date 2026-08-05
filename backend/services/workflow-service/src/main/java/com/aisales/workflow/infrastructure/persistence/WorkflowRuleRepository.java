package com.aisales.workflow.infrastructure.persistence;

import com.aisales.common.contracts.workflow.WorkflowTriggerType;
import com.aisales.workflow.domain.entity.WorkflowRule;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRuleRepository extends JpaRepository<WorkflowRule, UUID> {

    List<WorkflowRule> findByTenantIdAndTriggerTypeAndEnabledTrueAndDeletedAtIsNull(
            UUID tenantId, WorkflowTriggerType triggerType);

    Optional<WorkflowRule> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    List<WorkflowRule> findByTenantIdAndIdInAndDeletedAtIsNull(UUID tenantId, Collection<UUID> ids);

    Optional<WorkflowRule> findByTenantIdAndCodeAndDeletedAtIsNull(UUID tenantId, String code);

    List<WorkflowRule> findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID tenantId);
}
