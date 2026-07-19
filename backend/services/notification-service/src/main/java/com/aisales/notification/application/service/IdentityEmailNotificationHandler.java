package com.aisales.notification.application.service;

import com.aisales.common.contracts.notification.EmailTemplateCode;
import com.aisales.common.events.model.EmailVerificationRequestedEvent;
import com.aisales.common.events.model.PasswordResetRequestedEvent;
import com.aisales.notification.application.channel.NotificationDispatcher;
import com.aisales.notification.domain.channel.NotificationChannelType;
import com.aisales.notification.domain.channel.NotificationMessage;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Maps identity integration events to notification delivery.
 * Channel selection stays inside notification-service ({@link NotificationChannelType}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityEmailNotificationHandler {

    private final NotificationDispatcher notificationDispatcher;

    public void onEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        notificationDispatcher.dispatch(NotificationChannelType.EMAIL, NotificationMessage.builder()
                .tenantId(event.getTenantId())
                .recipient(event.getEmail())
                .templateCode(EmailTemplateCode.EMAIL_VERIFICATION)
                .variables(Map.of(
                        "firstName", event.getFirstName() != null ? event.getFirstName() : "there",
                        "token", event.getToken(),
                        "verificationLink", event.getVerificationLink()))
                .correlationId(event.getCorrelationId())
                .build());
        log.info("Processed EmailVerificationRequested eventId={} recipient={}",
                event.getEventId(), EmailDeliveryService.redactEmail(event.getEmail()));
    }

    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        notificationDispatcher.dispatch(NotificationChannelType.EMAIL, NotificationMessage.builder()
                .tenantId(event.getTenantId())
                .recipient(event.getEmail())
                .templateCode(EmailTemplateCode.PASSWORD_RESET)
                .variables(Map.of(
                        "firstName", event.getFirstName() != null ? event.getFirstName() : "there",
                        "token", event.getToken(),
                        "resetLink", event.getResetLink()))
                .correlationId(event.getCorrelationId())
                .build());
        log.info("Processed PasswordResetRequested eventId={} recipient={}",
                event.getEventId(), EmailDeliveryService.redactEmail(event.getEmail()));
    }
}
