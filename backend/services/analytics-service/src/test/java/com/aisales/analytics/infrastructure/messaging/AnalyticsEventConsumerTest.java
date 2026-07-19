package com.aisales.analytics.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.aisales.analytics.application.service.AnalyticsRecordingService;
import com.aisales.analytics.domain.AnalyticsMetricNames;
import com.aisales.common.events.consumer.IntegrationEventListener;
import com.aisales.common.events.model.KnowledgeRetrievedEvent;
import com.aisales.common.events.model.LeadCreatedEvent;
import com.aisales.common.events.model.LeadStatusChangedEvent;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventConsumerTest {

    @Mock private IntegrationEventListener integrationEventListener;
    @Mock private AnalyticsRecordingService recordingService;

    private AnalyticsEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AnalyticsEventConsumer(integrationEventListener, recordingService);
    }

    @Test
    void shouldRecordLeadCreated() {
        String tenantId = UUID.randomUUID().toString();
        LeadCreatedEvent event = LeadCreatedEvent.of(
                tenantId, UUID.randomUUID().toString(), "Ada", "WEB", "NEW", "corr-1");

        stubHandleIfType("LeadCreated", LeadCreatedEvent.class, event);

        consumer.onMessage(record());

        verify(recordingService).recordCount(
                eq(tenantId),
                eq(AnalyticsMetricNames.LEAD_CREATED),
                any(),
                eq("corr-1"),
                any());
    }

    @Test
    void shouldRecordLeadLostWhenStatusIsLost() {
        String tenantId = UUID.randomUUID().toString();
        LeadStatusChangedEvent event = LeadStatusChangedEvent.of(
                tenantId,
                UUID.randomUUID().toString(),
                "Ada",
                "CONTACTED",
                "LOST",
                "no budget",
                "corr-lost");

        stubHandleIfType("LeadStatusChanged", LeadStatusChangedEvent.class, event);

        consumer.onMessage(record());

        verify(recordingService).recordCount(
                eq(tenantId),
                eq(AnalyticsMetricNames.LEAD_STATUS_CHANGED),
                any(),
                eq("corr-lost"),
                any());
        verify(recordingService).recordCount(
                eq(tenantId),
                eq(AnalyticsMetricNames.LEAD_LOST),
                any(),
                eq("corr-lost"),
                any());
        verify(recordingService, times(2)).recordCount(any(), any(), any(), any(), any());
    }

    @Test
    void shouldRecordRagRequestOnKnowledgeRetrieved() {
        String tenantId = UUID.randomUUID().toString();
        KnowledgeRetrievedEvent event = KnowledgeRetrievedEvent.of(
                tenantId,
                UUID.randomUUID().toString(),
                "HYBRID",
                "5",
                "query-hash",
                "corr-rag");

        stubHandleIfType("KnowledgeRetrieved", KnowledgeRetrievedEvent.class, event);

        consumer.onMessage(record());

        verify(recordingService).recordCount(
                eq(tenantId),
                eq(AnalyticsMetricNames.RAG_REQUEST),
                any(),
                eq("corr-rag"),
                any());
    }

    @Test
    void shouldIgnoreWhenNoMatchingType() {
        doAnswer(invocation -> null)
                .when(integrationEventListener)
                .handleIfType(any(), any(), any(), any(), any());

        consumer.onMessage(record());

        verifyNoInteractions(recordingService);
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
