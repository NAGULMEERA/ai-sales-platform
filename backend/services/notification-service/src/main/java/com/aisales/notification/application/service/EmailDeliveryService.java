package com.aisales.notification.application.service;

import com.aisales.common.contracts.notification.SendTransactionalEmailRequest;
import com.aisales.notification.domain.enums.EmailTemplateCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDeliveryService {

    private final EmailTemplateRenderer templateRenderer;
    private final NotificationProperties notificationProperties;
    private final JavaMailSender mailSender;

    public void sendTransactionalEmail(SendTransactionalEmailRequest request) {
        EmailTemplateCode templateCode = EmailTemplateCode.from(request.getTemplateCode());
        Map<String, String> variables = request.getVariables() == null ? Map.of() : request.getVariables();
        EmailTemplateRenderer.RenderedEmail rendered = templateRenderer.render(templateCode, variables);

        if ("smtp".equalsIgnoreCase(notificationProperties.getDeliveryMode())) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(notificationProperties.getFromAddress());
            message.setTo(request.getRecipientEmail());
            message.setSubject(rendered.subject());
            message.setText(rendered.body());
            mailSender.send(message);
            log.info("Email sent via SMTP to {} template={} tenantId={}",
                    request.getRecipientEmail(), request.getTemplateCode(), request.getTenantId());
            return;
        }

        log.info("""
                ================= EMAIL ({} mode) =================
                To: {}
                Tenant: {}
                Template: {}
                Subject: {}
                Body:
                {}
                ====================================================
                """,
                notificationProperties.getDeliveryMode(),
                request.getRecipientEmail(),
                request.getTenantId(),
                request.getTemplateCode(),
                rendered.subject(),
                rendered.body());
    }
}
