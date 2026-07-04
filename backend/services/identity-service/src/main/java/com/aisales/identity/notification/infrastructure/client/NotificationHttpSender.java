package com.aisales.identity.notification.infrastructure.client;

import com.aisales.common.contracts.notification.SendTransactionalEmailRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


/**
 * Performs the raw HTTP call to notification-service, decorated with the {@code
 * notificationService} retry policy (see {@code application.yml}: transient failures only,
 * exponential backoff with jitter, capped at 3 attempts - Rule 09/10).
 *
 * <p>Deliberately split out from {@link NotificationRestClient}: {@code @Retry} is AOP-proxy
 * based and only intercepts calls arriving from <i>outside</i> the bean (self-invocation bypasses
 * the proxy). {@link NotificationRestClient#sendTransactionalEmail} also conditionally swallows
 * failures for fallback logging - if retry were applied there directly, a swallowed exception
 * would look like a "successful" call to the retry aspect and no retry would ever happen. Placing
 * {@code @Retry} on this separate bean's method ensures retries are exhausted transparently
 * <i>before</i> the exception ever reaches {@link NotificationRestClient}'s fallback decision.
 *
 * <p>Also attaches a bearer token minted by {@link InternalServiceTokenProvider}: notification-service
 * requires authentication (including the {@code SERVICE} role) for this endpoint, since it is an
 * internal-only API never meant to be called by end users (Rule 04: internal APIs are consumed only
 * by microservices).
 */
@Component
public class NotificationHttpSender {

    private final RestTemplate restTemplate;
    private final InternalServiceTokenProvider internalServiceTokenProvider;

    public NotificationHttpSender(RestTemplate restTemplate, InternalServiceTokenProvider internalServiceTokenProvider) {
        this.restTemplate = restTemplate;
        this.internalServiceTokenProvider = internalServiceTokenProvider;
    }

    @CircuitBreaker(name = "notificationService")
    @Retry(name = "notificationService")
    public void send(String baseUrl, SendTransactionalEmailRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(internalServiceTokenProvider.mintServiceToken());
        HttpEntity<SendTransactionalEmailRequest> entity = new HttpEntity<>(request, headers);
        restTemplate.postForEntity(baseUrl + "/api/v1/notifications/email", entity, Void.class);
    }
}
