package com.aisales.common.observability.util;

import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import org.slf4j.MDC;

public final class MDCUtils {

    private MDCUtils() {
    }

    public static void putContext() {
        CorrelationIdUtils.get().ifPresent(id -> MDC.put(ApiConstants.CORRELATION_ID_HEADER, id));
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            MDC.put("tenantId", tenantId);
        }
    }

    public static void clearContext() {
        MDC.remove(ApiConstants.CORRELATION_ID_HEADER);
        MDC.remove("tenantId");
    }
}
