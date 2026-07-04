package com.aisales.identity.notification.application;

import com.aisales.common.contracts.notification.EmailTemplateCode;
import com.aisales.common.contracts.notification.SendTransactionalEmailRequest;
import com.aisales.common.core.util.CorrelationIdUtils;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.aisales.identity.authentication.infrastructure.configuration.AuthProperties;
import com.aisales.identity.notification.infrastructure.client.NotificationRestClient;



@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final NotificationRestClient notificationRestClient;
    private final AuthProperties authProperties;

    public void sendVerificationEmail(UUID tenantId, String email, String firstName, String token) {
        String verificationLink = authProperties.getVerificationLinkBaseUrl() + "?token=" + token;
        SendTransactionalEmailRequest request = SendTransactionalEmailRequest.builder()
                .tenantId(tenantId != null ? tenantId.toString() : null)
                .recipientEmail(email)
                .templateCode(EmailTemplateCode.EMAIL_VERIFICATION)
                .variables(Map.of(
                        "firstName", firstName != null ? firstName : "there",
                        "token", token,
                        "verificationLink", verificationLink))
                .correlationId(CorrelationIdUtils.getCorrelationId())
                .build();
        notificationRestClient.sendTransactionalEmail(request);
        log.info("Verification email requested for {}", email);
    }

    public void sendPasswordResetEmail(UUID tenantId, String email, String firstName, String token) {
        String resetLink = authProperties.getPasswordResetLinkBaseUrl() + "?token=" + token;
        SendTransactionalEmailRequest request = SendTransactionalEmailRequest.builder()
                .tenantId(tenantId != null ? tenantId.toString() : null)
                .recipientEmail(email)
                .templateCode(EmailTemplateCode.PASSWORD_RESET)
                .variables(Map.of(
                        "firstName", firstName != null ? firstName : "there",
                        "token", token,
                        "resetLink", resetLink))
                .correlationId(CorrelationIdUtils.getCorrelationId())
                .build();
        notificationRestClient.sendTransactionalEmail(request);
        log.info("Password reset email requested for {}", email);
    }
}
