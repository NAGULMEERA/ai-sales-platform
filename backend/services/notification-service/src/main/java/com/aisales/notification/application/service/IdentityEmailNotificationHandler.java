package com.aisales.notification.application.service;

import com.aisales.common.contracts.notification.EmailTemplateCode;
import com.aisales.common.contracts.notification.SendTransactionalEmailRequest;
import com.aisales.common.events.model.EmailVerificationRequestedEvent;
import com.aisales.common.events.model.PasswordResetRequestedEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Maps identity integration events to transactional email delivery.
 * Channel choice (email vs future SMS/WhatsApp) stays inside notification-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityEmailNotificationHandler {

    private final EmailDeliveryService emailDeliveryService;

    public void onEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        emailDeliveryService.sendTransactionalEmail(SendTransactionalEmailRequest.builder()
                .tenantId(event.getTenantId())
                .recipientEmail(event.getEmail())
                .templateCode(EmailTemplateCode.EMAIL_VERIFICATION)
                .variables(Map.of(
                        "firstName", event.getFirstName() != null ? event.getFirstName() : "there",
                        "token", event.getToken(),
                        "verificationLink", event.getVerificationLink()))
                .correlationId(event.getCorrelationId())
                .build());
        log.info("Processed EmailVerificationRequested eventId={} recipient={}",
                event.getEventId(), event.getEmail());
    }

    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        emailDeliveryService.sendTransactionalEmail(SendTransactionalEmailRequest.builder()
                .tenantId(event.getTenantId())
                .recipientEmail(event.getEmail())
                .templateCode(EmailTemplateCode.PASSWORD_RESET)
                .variables(Map.of(
                        "firstName", event.getFirstName() != null ? event.getFirstName() : "there",
                        "token", event.getToken(),
                        "resetLink", event.getResetLink()))
                .correlationId(event.getCorrelationId())
                .build());
        log.info("Processed PasswordResetRequested eventId={} recipient={}",
                event.getEventId(), event.getEmail());
    }
}
