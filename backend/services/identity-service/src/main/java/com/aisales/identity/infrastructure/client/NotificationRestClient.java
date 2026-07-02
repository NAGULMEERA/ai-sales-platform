package com.aisales.identity.infrastructure.client;

import com.aisales.common.contracts.notification.SendTransactionalEmailRequest;
import com.aisales.common.observability.http.OutboundCallDiagnostics;
import com.aisales.identity.infrastructure.configuration.NotificationClientProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRestClient {

    private static final String TARGET_SERVICE = "notification-service";

    private final NotificationHttpSender notificationHttpSender;
    private final NotificationClientProperties properties;

    public void sendTransactionalEmail(SendTransactionalEmailRequest request) {
        if (!properties.isEnabled()) {
            log.info("Notification client disabled; email not sent to {}", request.getRecipientEmail());
            return;
        }
        long startedAtMs = System.currentTimeMillis();
        try {
            // Transient failures (timeouts, connection errors, 5xx/429) are already retried with
            // exponential backoff inside notificationHttpSender before an exception ever reaches
            // this catch block - see NotificationHttpSender for why retry lives there and not here.
            notificationHttpSender.send(properties.getBaseUrl(), request);
            log.debug("Outbound call to notification-service succeeded {} {}",
                    StructuredArguments.kv("target_service", TARGET_SERVICE),
                    StructuredArguments.kv("elapsed_ms", OutboundCallDiagnostics.elapsedMillisSince(startedAtMs)));
        } catch (RestClientException ex) {
            long elapsedMs = OutboundCallDiagnostics.elapsedMillisSince(startedAtMs);
            String outcome = OutboundCallDiagnostics.outcome(ex);
            if (properties.isFallbackLogging()) {
                log.warn("Outbound call to notification-service failed; email was not delivered remotely "
                                + "{} {} {} {}",
                        StructuredArguments.kv("target_service", TARGET_SERVICE),
                        StructuredArguments.kv("elapsed_ms", elapsedMs),
                        StructuredArguments.kv("outcome", outcome),
                        StructuredArguments.kv("error", ex.getMessage()));
            } else {
                log.error("Outbound call to notification-service failed {} {} {} {}",
                        StructuredArguments.kv("target_service", TARGET_SERVICE),
                        StructuredArguments.kv("elapsed_ms", elapsedMs),
                        StructuredArguments.kv("outcome", outcome),
                        StructuredArguments.kv("error", ex.getMessage()));
                throw ex;
            }
        }
    }
}
