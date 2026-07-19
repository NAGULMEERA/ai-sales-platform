package com.aisales.notification.application.service;

import com.aisales.common.contracts.notification.SendTransactionalEmailRequest;
import com.aisales.common.observability.http.OutboundCallDiagnostics;
import com.aisales.notification.domain.enums.EmailTemplateCode;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.mail.MailException;
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
    private final MailProperties mailProperties;

    @Bulkhead(name = "smtpEmail")
    @CircuitBreaker(name = "smtpEmail")
    @Retry(name = "smtpEmail")
    public void sendTransactionalEmail(SendTransactionalEmailRequest request) {
        EmailTemplateCode templateCode = EmailTemplateCode.from(request.getTemplateCode());
        Map<String, String> variables = request.getVariables() == null ? Map.of() : request.getVariables();
        EmailTemplateRenderer.RenderedEmail rendered = templateRenderer.render(templateCode, variables);

        if ("smtp".equalsIgnoreCase(notificationProperties.getDeliveryMode())) {
            String targetService = "smtp:" + mailProperties.getHost() + ":" + mailProperties.getPort();
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(notificationProperties.getFromAddress());
            message.setTo(request.getRecipientEmail());
            message.setSubject(rendered.subject());
            message.setText(rendered.body());
            long startedAtMs = System.currentTimeMillis();
            try {
                mailSender.send(message);
            } catch (MailException ex) {
                log.error("Outbound SMTP call failed {} {} {} {}",
                        StructuredArguments.kv("target_service", targetService),
                        StructuredArguments.kv("elapsed_ms", OutboundCallDiagnostics.elapsedMillisSince(startedAtMs)),
                        StructuredArguments.kv("outcome", OutboundCallDiagnostics.outcome(ex)),
                        StructuredArguments.kv("error", ex.getMessage()));
                throw ex;
            }
            log.info("Email sent via SMTP template={} tenantId={} recipient={} {} {}",
                    request.getTemplateCode(),
                    request.getTenantId(),
                    redactEmail(request.getRecipientEmail()),
                    StructuredArguments.kv("target_service", targetService),
                    StructuredArguments.kv("elapsed_ms", OutboundCallDiagnostics.elapsedMillisSince(startedAtMs)));
            return;
        }

        // Never log subject/body — templates embed verification/reset tokens and links.
        log.info(
                "Email rendered ({} mode) template={} tenantId={} recipient={} subject_len={} body_len={}",
                notificationProperties.getDeliveryMode(),
                request.getTemplateCode(),
                request.getTenantId(),
                redactEmail(request.getRecipientEmail()),
                rendered.subject() != null ? rendered.subject().length() : 0,
                rendered.body() != null ? rendered.body().length() : 0);
    }

    /** Redacts local-part for structured logs (keeps domain for ops diagnosis). */
    static String redactEmail(String email) {
        if (email == null || email.isBlank()) {
            return "(none)";
        }
        int at = email.indexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return "***";
        }
        return "***@" + email.substring(at + 1);
    }
}
