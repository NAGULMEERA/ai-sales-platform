package com.aisales.workflow.domain.entity;

import com.aisales.common.core.audit.BaseEntity;
import com.aisales.workflow.domain.enums.WorkflowDefinitionKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workflow_execution")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecution extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "definition_key", nullable = false, length = 100)
    private WorkflowDefinitionKey definitionKey;

    @Column(name = "business_key", nullable = false)
    private String businessKey;

    /** Workflow-specific state name (e.g. OnboardingState / LeadLifecycleState). */
    @Column(nullable = false, length = 64)
    private String state;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
