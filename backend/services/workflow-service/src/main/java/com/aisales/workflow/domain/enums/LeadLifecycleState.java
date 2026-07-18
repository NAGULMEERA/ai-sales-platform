package com.aisales.workflow.domain.enums;

/**
 * Execution states for {@link WorkflowDefinitionKey#LEAD_LIFECYCLE_V1}.
 * Business lead status remains owned by lead-service.
 */
public enum LeadLifecycleState {
    CREATED,
    AWAITING_VALIDATION,
    VALIDATED,
    QUALIFIED,
    ASSIGNED,
    COMPLETED,
    FAILED
}
