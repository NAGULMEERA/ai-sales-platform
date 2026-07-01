package com.aisales.common.observability.tracing;

import java.util.UUID;

public final class TraceIdGenerator {

    private TraceIdGenerator() {
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
