package com.aisales.common.events.kafka;

import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.BaseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Component
public class EventKafkaHeaderPropagator {

    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final Propagator propagator;

    public EventKafkaHeaderPropagator(
            ObjectMapper objectMapper,
            @Autowired(required = false) Tracer tracer,
            @Autowired(required = false) Propagator propagator) {
        this.objectMapper = objectMapper;
        this.tracer = tracer;
        this.propagator = propagator;
    }

    public ProducerRecord<String, String> enrichProducerRecord(String topic, String key, String payload) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);
        applyPayloadHeaders(record.headers(), payload);
        injectTraceContext(record.headers());
        return record;
    }

    public Span startConsumerSpan(ConsumerRecord<String, String> record, String operationName) {
        if (tracer == null) {
            return null;
        }
        if (propagator != null) {
            Span.Builder extracted = propagator.extract(record.headers(), EventKafkaHeaderPropagator::headerValue);
            if (extracted != null) {
                return extracted.name(operationName).start();
            }
        }
        return tracer.nextSpan();
    }

    public AutoCloseable activateSpan(Span span) {
        if (tracer == null || span == null) {
            return () -> {
            };
        }
        Tracer.SpanInScope scope = tracer.withSpan(span);
        return scope::close;
    }

    public void endSpan(Span span) {
        if (span != null) {
            span.end();
        }
    }

    public void applyConsumerHeaders(ConsumerRecord<String, String> record) {
        Headers headers = record.headers();
        String correlationId = headerValue(headers, EventKafkaHeaders.CORRELATION_ID);
        if (StringUtils.hasText(correlationId)) {
            CorrelationIdUtils.setCorrelationId(correlationId);
        }
        String tenantId = headerValue(headers, EventKafkaHeaders.TENANT_ID);
        if (StringUtils.hasText(tenantId)) {
            TenantContext.setTenantId(tenantId);
        }
    }

    public void applyPayloadHeaders(Headers headers, String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            addHeader(headers, EventKafkaHeaders.EVENT_ID, textOrNull(node, "eventId"));
            addHeader(headers, EventKafkaHeaders.EVENT_TYPE, textOrNull(node, "eventType"));
            addHeader(headers, EventKafkaHeaders.EVENT_VERSION, eventVersion(node));
            addHeader(headers, EventKafkaHeaders.CORRELATION_ID, textOrNull(node, "correlationId"));
            addHeader(headers, EventKafkaHeaders.TENANT_ID, textOrNull(node, "tenantId"));
        } catch (Exception ignored) {
            // Payload may not be JSON; headers remain optional.
        }
    }

    private void injectTraceContext(Headers headers) {
        if (tracer == null || propagator == null) {
            return;
        }
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return;
        }
        propagator.inject(currentSpan.context(), headers, (carrier, key, value) ->
                carrier.add(key, value.getBytes(StandardCharsets.UTF_8)));
    }

    private static String headerValue(Headers headers, String name) {
        var header = headers.lastHeader(name);
        if (header == null || header.value() == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private static void addHeader(Headers headers, String name, String value) {
        if (StringUtils.hasText(value)) {
            headers.add(name, value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }

    private static String eventVersion(JsonNode node) {
        String eventVersion = textOrNull(node, "eventVersion");
        return StringUtils.hasText(eventVersion) ? eventVersion : textOrNull(node, "version");
    }

    public void applyEventContext(BaseEvent event) {
        if (event.getCorrelationId() != null) {
            CorrelationIdUtils.setCorrelationId(event.getCorrelationId());
        }
        if (event.getTenantId() != null) {
            TenantContext.setTenantId(event.getTenantId());
        }
        Span currentSpan = tracer != null ? tracer.currentSpan() : null;
        if (currentSpan != null) {
            tagIfPresent(currentSpan, "tenant.id", event.getTenantId());
            tagIfPresent(currentSpan, "request.id", event.getCorrelationId());
            tagIfPresent(currentSpan, "event.id", event.getEventId());
            tagIfPresent(currentSpan, "event.type", event.getEventType());
            currentSpan.tag("event.version", String.valueOf(event.getEventVersion()));
        }
    }

    private static void tagIfPresent(Span span, String name, String value) {
        if (StringUtils.hasText(value)) {
            span.tag(name, value);
        }
    }
}
