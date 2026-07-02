package com.aisales.common.core.util;

import com.aisales.common.core.constant.ApiConstants;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;

public final class CorrelationIdUtils {

    private CorrelationIdUtils() {
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    public static void set(String correlationId) {
        MDC.put(ApiConstants.CORRELATION_ID_HEADER, correlationId);
        MDC.put(MDCUtils.CORRELATION_ID_KEY, correlationId);
    }

    public static void setCorrelationId(String correlationId) {
        set(correlationId);
    }

    public static Optional<String> get() {
        return Optional.ofNullable(MDC.get(ApiConstants.CORRELATION_ID_HEADER));
    }

    public static String getCorrelationId() {
        return get().orElse(null);
    }

    public static void clear() {
        MDC.remove(ApiConstants.CORRELATION_ID_HEADER);
        MDC.remove(MDCUtils.CORRELATION_ID_KEY);
    }
}
