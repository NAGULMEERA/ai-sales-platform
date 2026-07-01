package com.aisales.common.events.consumer;

import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.BaseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseEventConsumer<T extends BaseEvent> {

    private final ObjectMapper objectMapper;
    private final Class<T> eventClass;

    protected void handleMessage(String payload) {
        try {
            T event = objectMapper.readValue(payload, eventClass);
            if (event.getCorrelationId() != null) {
                CorrelationIdUtils.setCorrelationId(event.getCorrelationId());
            }
            if (event.getTenantId() != null) {
                TenantContext.setTenantId(event.getTenantId());
            }
            processEvent(event);
        } catch (Exception e) {
            log.error("Failed to process event of type {}", eventClass.getSimpleName(), e);
            onError(payload, e);
        } finally {
            TenantContext.clear();
            CorrelationIdUtils.clear();
        }
    }

    protected abstract void processEvent(T event);

    protected void onError(String payload, Exception exception) {
        log.warn("Event processing error for payload: {}", payload);
    }
}
