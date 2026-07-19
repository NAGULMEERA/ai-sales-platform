package com.aisales.search.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.aisales.common.contracts.search.SearchEntityType;
import com.aisales.common.events.consumer.IntegrationEventListener;
import com.aisales.common.events.model.CustomerCreatedEvent;
import com.aisales.common.events.model.LeadCreatedEvent;
import com.aisales.common.events.model.LeadStatusChangedEvent;
import com.aisales.search.application.service.SearchIndexingService;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchIndexEventConsumerTest {

    @Mock private IntegrationEventListener integrationEventListener;
    @Mock private SearchIndexingService indexingService;

    private SearchIndexEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SearchIndexEventConsumer(integrationEventListener, indexingService);
    }

    @Test
    void shouldIndexLeadCreated() {
        UUID tenantId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        LeadCreatedEvent event = LeadCreatedEvent.of(
                tenantId.toString(), leadId.toString(), "Ada Lead", "WEB", "NEW", "corr-1");

        stubHandleIfType("LeadCreated", LeadCreatedEvent.class, event);

        consumer.onMessage(record());

        verify(indexingService).upsert(
                eq(tenantId),
                isNull(),
                eq(SearchEntityType.LEAD),
                eq(leadId),
                eq("Ada Lead"),
                any(),
                eq("WEB"),
                eq("NEW"),
                eq("WEB"),
                eq(0d),
                eq(0L),
                any(),
                any());
    }

    @Test
    void shouldIndexLeadStatusChanged() {
        UUID tenantId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        LeadStatusChangedEvent event = LeadStatusChangedEvent.of(
                tenantId.toString(),
                leadId.toString(),
                "Ada Lead",
                "NEW",
                "CONTACTED",
                "first touch",
                "corr-2");

        stubHandleIfType("LeadStatusChanged", LeadStatusChangedEvent.class, event);

        consumer.onMessage(record());

        verify(indexingService).upsert(
                eq(tenantId),
                isNull(),
                eq(SearchEntityType.LEAD),
                eq(leadId),
                eq("Ada Lead"),
                any(),
                isNull(),
                eq("CONTACTED"),
                isNull(),
                isNull(),
                eq(0L),
                any(),
                any());
    }

    @Test
    void shouldIndexCustomerCreated() {
        UUID tenantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        CustomerCreatedEvent event = CustomerCreatedEvent.of(
                tenantId.toString(), customerId.toString(), "Ada Customer", "ada@example.com", "corr-3");

        stubHandleIfType("CustomerCreated", CustomerCreatedEvent.class, event);

        consumer.onMessage(record());

        verify(indexingService).upsert(
                eq(tenantId),
                isNull(),
                eq(SearchEntityType.CUSTOMER),
                eq(customerId),
                eq("Ada Customer"),
                any(),
                eq("ada@example.com"),
                eq("ACTIVE"),
                isNull(),
                eq(50d),
                eq(0L),
                any(),
                any());
    }

    @Test
    void shouldIgnoreWhenNoMatchingType() {
        doAnswer(invocation -> null)
                .when(integrationEventListener)
                .handleIfType(any(), any(), any(), any(), any());

        consumer.onMessage(record());

        verifyNoInteractions(indexingService);
    }

    @SuppressWarnings("unchecked")
    private <T> void stubHandleIfType(String expectedType, Class<T> type, T event) {
        doAnswer(invocation -> {
                    String actualExpected = invocation.getArgument(2);
                    Class<?> eventClass = invocation.getArgument(3);
                    Consumer<Object> handler = invocation.getArgument(4);
                    if (expectedType.equals(actualExpected) && type.equals(eventClass)) {
                        handler.accept(event);
                    }
                    return null;
                })
                .when(integrationEventListener)
                .handleIfType(any(), any(), any(), any(), any());
    }

    private static ConsumerRecord<String, String> record() {
        return new ConsumerRecord<>("aisales-events", 0, 0L, "key", "{}");
    }
}
