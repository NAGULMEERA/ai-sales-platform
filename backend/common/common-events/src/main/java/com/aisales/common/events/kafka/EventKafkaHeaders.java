package com.aisales.common.events.kafka;

import com.aisales.common.core.constant.ApiConstants;

public final class EventKafkaHeaders {

    public static final String CORRELATION_ID = ApiConstants.CORRELATION_ID_HEADER;
    public static final String TENANT_ID = ApiConstants.TENANT_ID_HEADER;
    public static final String EVENT_ID = "X-Event-Id";
    public static final String EVENT_TYPE = "X-Event-Type";
    public static final String EVENT_VERSION = "X-Event-Version";

    private EventKafkaHeaders() {
    }
}
