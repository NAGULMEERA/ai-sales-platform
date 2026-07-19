package com.aisales.workflow.infrastructure.messaging;

import com.aisales.common.events.consumer.IntegrationEventListener;
import com.aisales.common.events.model.EmailVerifiedEvent;
import com.aisales.common.events.model.UserCreatedEvent;
import com.aisales.workflow.application.service.OnboardingWorkflowService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.events.inbox.enabled", havingValue = "true", matchIfMissing = true)
public class OnboardingEventConsumer {

    static final String USER_CREATED_CONSUMER = "workflow-onboarding-user-created";
    static final String EMAIL_VERIFIED_CONSUMER = "workflow-onboarding-email-verified";

    private final IntegrationEventListener integrationEventListener;
    private final OnboardingWorkflowService onboardingWorkflowService;

    @KafkaListener(
            topics = "${aisales.events.default-topic:aisales-events}",
            groupId = "workflow-service-onboarding",
            containerFactory = "integrationKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, String> record) {
        integrationEventListener.handleIfType(
                record,
                USER_CREATED_CONSUMER,
                UserCreatedEvent.EVENT_TYPE, UserCreatedEvent.class,
                event -> onboardingWorkflowService.startOnUserCreated(
                        event.getTenantId(), event.getAggregateId(), event.getCorrelationId()));
        integrationEventListener.handleIfType(
                record,
                EMAIL_VERIFIED_CONSUMER,
                EmailVerifiedEvent.EVENT_TYPE, EmailVerifiedEvent.class,
                event -> onboardingWorkflowService.completeOnEmailVerified(
                        event.getTenantId(), event.getAggregateId(), event.getCorrelationId()));
    }
}
