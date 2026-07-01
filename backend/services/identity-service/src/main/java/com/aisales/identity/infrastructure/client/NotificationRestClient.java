package com.aisales.identity.infrastructure.client;

import com.aisales.common.contracts.notification.SendTransactionalEmailRequest;
import com.aisales.identity.infrastructure.configuration.NotificationClientProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRestClient {

    private final RestTemplate restTemplate;
    private final NotificationClientProperties properties;

    public void sendTransactionalEmail(SendTransactionalEmailRequest request) {
        if (!properties.isEnabled()) {
            log.info("Notification client disabled; email not sent to {}", request.getRecipientEmail());
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SendTransactionalEmailRequest> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(
                    properties.getBaseUrl() + "/api/v1/notifications/email",
                    entity,
                    Void.class);
        } catch (RestClientException ex) {
            if (properties.isFallbackLogging()) {
                log.warn("Notification service call failed ({}). Email was not delivered remotely.",
                        ex.getMessage());
            } else {
                throw ex;
            }
        }
    }
}
