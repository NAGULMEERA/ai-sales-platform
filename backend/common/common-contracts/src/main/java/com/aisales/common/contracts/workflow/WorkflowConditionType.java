package com.aisales.common.contracts.workflow;

public enum WorkflowConditionType {
    LEAD_SCORE_GT,
    OPPORTUNITY_STAGE_EQUALS,
    CUSTOMER_EXISTS,
    CONVERSATION_IDLE_MINUTES,
    NO_REPLY_MINUTES,
    AI_CONFIDENCE_GTE,
    CATALOG_AVAILABLE,
    BUSINESS_HOURS,
    TENANT_PLAN_EQUALS,
    ALWAYS
}
