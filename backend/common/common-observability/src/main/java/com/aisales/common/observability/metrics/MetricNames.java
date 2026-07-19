package com.aisales.common.observability.metrics;

/**
 * Platform-wide business metric names. Use snake_case; Prometheus export converts dots if used.
 */
public final class MetricNames {

    // Tenant lifecycle
    public static final String TENANT_CREATED = "tenant_created_total";
    public static final String TENANT_UPDATED = "tenant_updated_total";
    public static final String TENANT_DELETED = "tenant_deleted_total";

    // Lead lifecycle (Lead Service)
    public static final String LEAD_CREATED = "lead_created_total";
    public static final String LEAD_UPDATED = "lead_updated_total";
    public static final String LEAD_QUALIFIED = "lead_qualified_total";
    public static final String LEAD_ASSIGNED = "lead_assigned_total";
    public static final String LEAD_UNASSIGNED = "lead_unassigned_total";
    public static final String LEAD_CONVERTED = "lead_converted_total";
    public static final String LEAD_LOST = "lead_lost_total";
    public static final String LEAD_REOPENED = "lead_reopened_total";
    public static final String LEAD_MERGED = "lead_merged_total";
    public static final String LEAD_ASSIGNMENT_DURATION = "lead_assignment_duration";

    // Customer lifecycle (Customer Service)
    public static final String CUSTOMER_CREATED = "customer_created_total";
    public static final String CUSTOMER_UPDATED = "customer_updated_total";
    public static final String CUSTOMER_MERGED = "customer_merged_total";
    public static final String CUSTOMER_VERIFIED = "customer_verified_total";
    public static final String CUSTOMER_DEACTIVATED = "customer_deactivated_total";
    public static final String CUSTOMER_REACTIVATED = "customer_reactivated_total";
    public static final String CUSTOMER_CONVERTED = "customer_converted_total";

    // Events
    public static final String EVENT_PUBLISHED = "platform_event_published_total";
    public static final String EVENT_CONSUMED = "platform_event_consumed_total";
    public static final String EVENT_DLQ = "platform_event_dlq_total";
    public static final String KAFKA_PUBLISH_LATENCY = "platform_kafka_publish_latency";
    public static final String OUTBOX_PENDING = "platform_outbox_pending";
    public static final String OUTBOX_DISPATCHING = "platform_outbox_dispatching";
    public static final String OUTBOX_FAILED = "platform_outbox_failed";
    public static final String OUTBOX_DISPATCHED = "platform_outbox_dispatched_total";

    // Auth / JWT
    public static final String AUTH_LOGIN = "platform_auth_login_total";
    public static final String JWT_VALIDATION_FAILURE = "platform_jwt_validation_failures_total";

    // Notification
    public static final String NOTIFICATION_DELIVERY = "platform_notification_delivery_total";

    // HTTP
    public static final String HTTP_SLOW_REQUEST = "http_server_slow_requests_total";
    public static final String HTTP_SERVER_ERROR = "http_server_errors_total";
    public static final String OUTBOUND_SLOW_CALL = "http_client_slow_calls_total";

    // AI
    public static final String AI_REQUEST = "ai_request_total";
    public static final String AI_TOKENS = "ai_tokens_total";
    public static final String AI_SLOW_REQUEST = "ai_slow_request_total";
    public static final String AI_CACHE_HIT = "ai_semantic_cache_hit_total";
    public static final String AI_CACHE_MISS = "ai_semantic_cache_miss_total";
    public static final String AI_QUALIFICATION = "ai_qualification_total";
    public static final String AI_KNOWLEDGE_RETRIEVED = "ai_knowledge_retrieved_total";

    // Billing
    public static final String BILLING_INVOICE_CREATED = "billing_invoice_created_total";
    public static final String BILLING_INVOICE_ISSUED = "billing_invoice_issued_total";
    public static final String BILLING_PAYMENT_CREATED = "billing_payment_created_total";
    public static final String BILLING_STRIPE_WEBHOOK = "billing_stripe_webhook_total";

    // Catalog
    public static final String CATALOG_MATCH = "catalog_match_total";
    public static final String CATALOG_RECOMMENDATION = "catalog_recommendation_total";
    public static final String CATALOG_PRODUCT_CREATED = "catalog_product_created_total";
    public static final String CATALOG_PRODUCT_UPDATED = "catalog_product_updated_total";

    // Opportunity / deal
    public static final String OPPORTUNITY_CREATED = "opportunity_created_total";
    public static final String OPPORTUNITY_WON = "opportunity_won_total";
    public static final String OPPORTUNITY_LOST = "opportunity_lost_total";
    public static final String OPPORTUNITY_STAGE_CHANGED = "opportunity_stage_changed_total";
    public static final String OPPORTUNITY_CONVERSION = "opportunity_conversion_total";

    // Conversation
    public static final String CONVERSATION_STARTED = "conversation_started_total";
    public static final String CONVERSATION_CLOSED = "conversation_closed_total";
    public static final String MESSAGE_SENT = "message_sent_total";
    public static final String MESSAGE_RECEIVED = "message_received_total";
    public static final String CONVERSATION_AI_INSIGHT = "conversation_ai_insight_total";

    // Workflow automation
    public static final String WORKFLOW_EXECUTED = "workflow_executed_total";
    public static final String WORKFLOW_FAILED = "workflow_failed_total";
    public static final String WORKFLOW_DURATION = "workflow_duration";

    // Search
    public static final String SEARCH_REQUEST = "search_request_total";
    public static final String SEARCH_INDEXED = "search_indexed_total";
    public static final String SEARCH_DURATION = "search_duration";

    // Analytics
    public static final String ANALYTICS_EVENT_RECORDED = "analytics_event_recorded_total";
    public static final String ANALYTICS_DASHBOARD = "analytics_dashboard_total";

    // RAG (alias clarity for dashboards)
    public static final String RAG_REQUEST = "rag_request_total";

    private MetricNames() {
    }
}
