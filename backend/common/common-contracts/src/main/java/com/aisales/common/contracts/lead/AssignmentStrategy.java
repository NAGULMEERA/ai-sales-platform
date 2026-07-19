package com.aisales.common.contracts.lead;

public enum AssignmentStrategy {
    MANUAL,
    ROUND_ROBIN,
    /** Extension point — resolved via {@code LeadAiAssigneeResolver} when registered. */
    AI
}
