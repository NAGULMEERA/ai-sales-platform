package com.aisales.common.core.util;

import org.slf4j.MDC;

/**
 * Populates SLF4J's MDC with the current request/message context (correlation id,
 * tenant id, user id) so every structured log line carries these fields automatically.
 *
 * <p>Callers must invoke {@link #clearContext()} in a {@code finally} block to avoid
 * leaking context across thread-pool-reused threads.
 */
public final class MDCUtils {

    public static final String CORRELATION_ID_KEY = "correlation_id";
    public static final String TENANT_ID_KEY = "tenant_id";
    public static final String USER_ID_KEY = "user_id";

    private MDCUtils() {
    }

    /**
     * Copies the current {@link TenantContext} (tenant id, user id) into MDC.
     * Correlation id is managed separately by {@link CorrelationIdUtils}.
     */
    public static void putContext() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            MDC.put(TENANT_ID_KEY, tenantId);
        }
        String userId = TenantContext.getUserId();
        if (userId != null) {
            MDC.put(USER_ID_KEY, userId);
        }
    }

    public static void clearContext() {
        MDC.remove(TENANT_ID_KEY);
        MDC.remove(USER_ID_KEY);
    }
}
