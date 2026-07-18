package com.aisales.notification.infrastructure.messaging;

import com.aisales.common.events.consumer.IntegrationEventListener;
import com.aisales.common.events.model.EmailVerificationRequestedEvent;
import com.aisales.common.events.model.PasswordResetRequestedEvent;
import com.aisales.notification.application.service.IdentityEmailNotificationHandler;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes identity email-intent events from the shared platform topic.
 * Unrelated event types on the same topic are ignored via {@code handleIfType}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.events.inbox.enabled", havingValue = "true", matchIfMissing = true)
public class IdentityEmailEventConsumer {

    static final String EMAIL_VERIFICATION_CONSUMER = "notification-email-verification";
    static final String PASSWORD_RESET_CONSUMER = "notification-password-reset";

    private final IntegrationEventListener integrationEventListener;
    private final IdentityEmailNotificationHandler handler;

    @KafkaListener(
            topics = "${aisales.events.default-topic:aisales-events}",
            groupId = "notification-service-identity-email",
            containerFactory = "integrationKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, String> record) {
        integrationEventListener.handleIfType(
                record,
                EMAIL_VERIFICATION_CONSUMER,
                "EmailVerificationRequested",
                EmailVerificationRequestedEvent.class,
                handler::onEmailVerificationRequested);
        integrationEventListener.handleIfType(
                record,
                PASSWORD_RESET_CONSUMER,
                "PasswordResetRequested",
                PasswordResetRequestedEvent.class,
                handler::onPasswordResetRequested);
    }
}
