package com.aisales.common.events.consumer;

import com.aisales.common.events.inbox.DeadLetterService;
import com.aisales.common.events.inbox.InboxService;
import com.aisales.common.events.kafka.EventKafkaHeaderPropagator;
import com.aisales.common.events.model.TenantCreatedEvent;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    private final JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    private IntegrationEventListener integrationEventListener;

    @BeforeEach
    void setUp() {
        integrationEventListener = new IntegrationEventListener(
                objectMapper, inboxService, deadLetterService, headerPropagator, transactionManager());
        ReflectionTestUtils.setField(integrationEventListener, "maxAttempts", 3);
    }

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

    @Test
    void shouldDeadLetterEventWithoutId() throws Exception {
        TenantCreatedEvent event = TenantCreatedEvent.of(
                "tenant-1", "Acme", "acme", "FREE", "REAL_ESTATE", "corr-1");
        event.setEventId(null);
        String payload = objectMapper.writeValueAsString(event);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("aisales-events", 0, 0L, "tenant-1", payload);
        Consumer<TenantCreatedEvent> handler = mock(Consumer.class);

        integrationEventListener.handle(record, "tenant-service", TenantCreatedEvent.class, handler);

        verify(handler, never()).accept(any());
        verify(inboxService, never()).isProcessed(any(), any());
        verify(inboxService, never()).markProcessed(any(), any());
        verify(deadLetterService).recordFailure(eq(record), eq("tenant-service"), eq(null), eq("TenantCreated"),
                eq(0), any());
    }

    @Test
    void shouldRetryPoisonEventBeforeDeadLetter() throws Exception {
        TenantCreatedEvent event = TenantCreatedEvent.of(
                "tenant-1", "Acme", "acme", "FREE", "REAL_ESTATE", "corr-1");
        String payload = objectMapper.writeValueAsString(event);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("aisales-events", 0, 0L, "tenant-1", payload);
        Consumer<TenantCreatedEvent> handler = mock(Consumer.class);
        doThrow(new IllegalStateException("poison message")).when(handler).accept(any());
        when(inboxService.isProcessed(event.getEventId(), "tenant-service")).thenReturn(false);

        integrationEventListener.handle(record, "tenant-service", TenantCreatedEvent.class, handler);

        verify(handler, times(3)).accept(any());
        verify(inboxService, never()).markProcessed(any(), any());
        verify(deadLetterService).recordFailure(eq(record), eq("tenant-service"), eq(event.getEventId()),
                eq("TenantCreated"), eq(3), any());
    }

    private static AbstractPlatformTransactionManager transactionManager() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
            }
        };
    }
}
