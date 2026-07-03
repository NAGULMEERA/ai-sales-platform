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
    public static final String LEAD_QUALIFIED = "lead_qualified_total";
    public static final String LEAD_CONVERTED = "lead_converted_total";

    // Events
    public static final String EVENT_PUBLISHED = "platform_event_published_total";
    public static final String EVENT_CONSUMED = "platform_event_consumed_total";
    public static final String EVENT_DLQ = "platform_event_dlq_total";

    // HTTP
    public static final String HTTP_SLOW_REQUEST = "http_server_slow_requests_total";
    public static final String HTTP_SERVER_ERROR = "http_server_errors_total";
    public static final String OUTBOUND_SLOW_CALL = "http_client_slow_calls_total";

    // AI
    public static final String AI_REQUEST = "ai_request_total";
    public static final String AI_TOKENS = "ai_tokens_total";
    public static final String AI_SLOW_REQUEST = "ai_slow_request_total";

    private MetricNames() {
    }
}
