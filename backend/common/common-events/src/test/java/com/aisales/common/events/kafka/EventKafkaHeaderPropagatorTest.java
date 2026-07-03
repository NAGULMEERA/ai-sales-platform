package com.aisales.common.events.kafka;

import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.TenantCreatedEvent;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventKafkaHeaderPropagatorTest {

    private final JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final EventKafkaHeaderPropagator propagator =
            new EventKafkaHeaderPropagator(objectMapper, null, null);

    @AfterEach
    void tearDown() {
        CorrelationIdUtils.clear();
        TenantContext.clear();
    }

    @Test
    void shouldAddHeadersFromEventPayload() throws Exception {
        TenantCreatedEvent event = TenantCreatedEvent.of(
                "tenant-1", "Acme", "acme", "FREE", "REAL_ESTATE", "corr-123");
        String payload = objectMapper.writeValueAsString(event);

        ProducerRecord<String, String> record = propagator.enrichProducerRecord("aisales-events", "tenant-1", payload);

        assertThat(headerValue(record, EventKafkaHeaders.CORRELATION_ID)).isEqualTo("corr-123");
        assertThat(headerValue(record, EventKafkaHeaders.TENANT_ID)).isEqualTo("tenant-1");
        assertThat(headerValue(record, EventKafkaHeaders.EVENT_ID)).isEqualTo(event.getEventId());
        assertThat(headerValue(record, EventKafkaHeaders.EVENT_TYPE)).isEqualTo("TenantCreated");
        assertThat(headerValue(record, EventKafkaHeaders.EVENT_VERSION)).isEqualTo("1");
    }

    private String headerValue(ProducerRecord<String, String> record, String name) {
        var header = record.headers().lastHeader(name);
        return header != null ? new String(header.value()) : null;
    }
}
