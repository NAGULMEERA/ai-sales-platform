package com.aisales.common.events.consumer;

import com.aisales.common.events.inbox.DeadLetterService;
import com.aisales.common.events.inbox.InboxService;
import com.aisales.common.events.kafka.EventKafkaHeaderPropagator;
import com.aisales.common.events.model.TenantCreatedEvent;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationEventListenerTest {

    @Mock
    private InboxService inboxService;
    @Mock
    private DeadLetterService deadLetterService;
    @Mock
    private EventKafkaHeaderPropagator headerPropagator;

    @Spy
    private JsonMapper objectMapper = JsonMapper.builder().build();

    @InjectMocks
    private IntegrationEventListener integrationEventListener;

    @Test
    void shouldSkipDuplicateEvents() throws Exception {
        TenantCreatedEvent event = TenantCreatedEvent.of(
                "tenant-1", "Acme", "acme", "FREE", "REAL_ESTATE", "corr-1");
        String payload = objectMapper.writeValueAsString(event);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("aisales-events", 0, 0L, "tenant-1", payload);
        when(inboxService.isProcessed(event.getEventId(), "tenant-service")).thenReturn(true);

        integrationEventListener.handle(record, "tenant-service", TenantCreatedEvent.class, e -> {
            throw new IllegalStateException("should not process");
        });

        verify(inboxService, never()).markProcessed(any(), any());
    }

    @Test
    void shouldProcessAndMarkEvent() throws Exception {
        TenantCreatedEvent event = TenantCreatedEvent.of(
                "tenant-1", "Acme", "acme", "FREE", "REAL_ESTATE", "corr-1");
        String payload = objectMapper.writeValueAsString(event);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("aisales-events", 0, 0L, "tenant-1", payload);
        when(inboxService.isProcessed(event.getEventId(), "tenant-service")).thenReturn(false);

        integrationEventListener.handle(record, "tenant-service", TenantCreatedEvent.class, e -> {
        });

        verify(inboxService).markProcessed(eq(event.getEventId()), eq("tenant-service"));
    }
}
