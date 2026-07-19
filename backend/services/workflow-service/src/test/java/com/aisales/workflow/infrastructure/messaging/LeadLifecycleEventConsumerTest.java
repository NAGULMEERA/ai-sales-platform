package com.aisales.workflow.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.aisales.common.events.consumer.IntegrationEventListener;
import com.aisales.common.events.model.LeadAssignedEvent;
import com.aisales.common.events.model.LeadCreatedEvent;
import com.aisales.common.events.model.LeadQualifiedEvent;
import com.aisales.common.events.model.LeadValidatedEvent;
import com.aisales.workflow.application.service.LeadLifecycleWorkflowService;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadLifecycleEventConsumerTest {

    @Mock private IntegrationEventListener integrationEventListener;
    @Mock private LeadLifecycleWorkflowService leadLifecycleWorkflowService;

    private LeadLifecycleEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new LeadLifecycleEventConsumer(integrationEventListener, leadLifecycleWorkflowService);
    }

    @Test
    void shouldStartWorkflowOnLeadCreated() {
        String tenantId = UUID.randomUUID().toString();
        String leadId = UUID.randomUUID().toString();
        LeadCreatedEvent event = LeadCreatedEvent.of(tenantId, leadId, "Ada", "WEB", "NEW", "corr-1");

        stubHandleIfType("LeadCreated", LeadCreatedEvent.class, event);

        consumer.onMessage(record("LeadCreated"));

        verify(leadLifecycleWorkflowService).startOnLeadCreated(tenantId, leadId, "corr-1");
    }

    @Test
    void shouldAdvanceOnLeadValidated() {
        String tenantId = UUID.randomUUID().toString();
        String leadId = UUID.randomUUID().toString();
        LeadValidatedEvent event = LeadValidatedEvent.of(tenantId, leadId, "Ada", "corr-2");

        stubHandleIfType("LeadValidated", LeadValidatedEvent.class, event);

        consumer.onMessage(record("LeadValidated"));

        verify(leadLifecycleWorkflowService).onLeadValidated(tenantId, leadId, "corr-2");
    }

    @Test
    void shouldAdvanceOnLeadQualified() {
        String tenantId = UUID.randomUUID().toString();
        String leadId = UUID.randomUUID().toString();
        LeadQualifiedEvent event = LeadQualifiedEvent.of(tenantId, leadId, "Ada", 80, "QUALIFIED", "corr-3");

        stubHandleIfType("LeadQualified", LeadQualifiedEvent.class, event);

        consumer.onMessage(record("LeadQualified"));

        verify(leadLifecycleWorkflowService).onLeadQualified(tenantId, leadId, "corr-3");
    }

    @Test
    void shouldCompleteOnLeadAssigned() {
        String tenantId = UUID.randomUUID().toString();
        String leadId = UUID.randomUUID().toString();
        LeadAssignedEvent event =
                LeadAssignedEvent.of(tenantId, leadId, "Ada", "user-2", "round-robin", "corr-4");

        stubHandleIfType("LeadAssigned", LeadAssignedEvent.class, event);

        consumer.onMessage(record("LeadAssigned"));

        verify(leadLifecycleWorkflowService).completeOnLeadAssigned(tenantId, leadId, "corr-4");
    }

    @Test
    void shouldIgnoreUnrelatedEventTypes() {
        doAnswer(invocation -> null)
                .when(integrationEventListener)
                .handleIfType(any(), any(), any(), any(), any());

        consumer.onMessage(record("CustomerCreated"));

        verifyNoInteractions(leadLifecycleWorkflowService);
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

    private static ConsumerRecord<String, String> record(String eventType) {
        return new ConsumerRecord<>("aisales-events", 0, 0L, "key", "{\"eventType\":\"" + eventType + "\"}");
    }
}
