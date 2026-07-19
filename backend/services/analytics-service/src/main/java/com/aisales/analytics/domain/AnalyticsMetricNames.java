package com.aisales.analytics.domain;

/**
 * Canonical business fact names persisted in analytics_event.metric_name.
 */
public final class AnalyticsMetricNames {

    public static final String LEAD_CREATED = "lead.created";
    public static final String LEAD_QUALIFIED = "lead.qualified";
    public static final String LEAD_CONVERTED = "lead.converted";
    public static final String LEAD_LOST = "lead.lost";
    public static final String LEAD_STATUS_CHANGED = "lead.status_changed";

    public static final String CUSTOMER_CREATED = "customer.created";

    public static final String OPPORTUNITY_CREATED = "opportunity.created";
    public static final String OPPORTUNITY_WON = "opportunity.won";
    public static final String OPPORTUNITY_LOST = "opportunity.lost";
    public static final String OPPORTUNITY_STATUS_CHANGED = "opportunity.status_changed";

    public static final String CONVERSATION_STARTED = "conversation.started";
    public static final String MESSAGE_SENT = "message.sent";
    public static final String MESSAGE_RECEIVED = "message.received";
    public static final String CONVERSATION_RESPONSE_SECONDS = "conversation.response_seconds";

    public static final String WORKFLOW_EXECUTED = "workflow.executed";
    public static final String WORKFLOW_COMPLETED = "workflow.completed";
    public static final String WORKFLOW_FAILED = "workflow.failed";

    public static final String AI_REQUEST = "ai.request";
    public static final String AI_QUALIFICATION = "ai.qualification";
    public static final String AI_QUALIFICATION_ACCURACY = "ai.qualification_accuracy";

    public static final String CATALOG_MATCH = "catalog.match";
    public static final String CATALOG_RECOMMENDATION = "catalog.recommendation";
    public static final String CATALOG_RECOMMENDATION_ACCURACY = "catalog.recommendation_accuracy";

    public static final String SEARCH_REQUEST = "search.request";
    public static final String RAG_REQUEST = "rag.request";

    private AnalyticsMetricNames() {
    }
}
