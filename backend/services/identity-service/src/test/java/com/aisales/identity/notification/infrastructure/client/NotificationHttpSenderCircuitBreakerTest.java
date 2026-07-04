package com.aisales.identity.notification.infrastructure.client;

import com.aisales.common.contracts.notification.SendTransactionalEmailRequest;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;


import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code notificationService} circuit breaker opens after sustained dependency
 * failures and fast-fails subsequent calls without hitting the downstream client again.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NotificationHttpSender.class)
@ImportAutoConfiguration({
        AopAutoConfiguration.class,
        RetryAutoConfiguration.class,
        CircuitBreakerAutoConfiguration.class
})
@TestPropertySource(properties = {
        "resilience4j.retry.instances.notificationService.max-attempts=1",
        "resilience4j.retry.instances.notificationService.retry-exception-predicate="
                + "com.aisales.common.observability.http.TransientFailurePredicate",
        "resilience4j.circuitbreaker.instances.notificationService.sliding-window-size=2",
        "resilience4j.circuitbreaker.instances.notificationService.minimum-number-of-calls=2",
        "resilience4j.circuitbreaker.instances.notificationService.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.notificationService.wait-duration-in-open-state=600s",
        "resilience4j.circuitbreaker.instances.notificationService.record-failure-predicate="
                + "com.aisales.common.observability.http.TransientFailurePredicate"
})
class NotificationHttpSenderCircuitBreakerTest {

    @Autowired
    private NotificationHttpSender sender;

    @MockitoBean
    private RestTemplate restTemplate;

    @MockitoBean
    private InternalServiceTokenProvider internalServiceTokenProvider;

    @BeforeEach
    void stubServiceToken() {
        when(internalServiceTokenProvider.mintServiceToken()).thenReturn("test-service-token");
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenThrow(new ResourceAccessException("connection reset"));
    }

    @Test
    void shouldOpenCircuitAndFastFailAfterSustainedDependencyFailures() {
        assertThatThrownBy(() -> sender.send("http://notification-service", request()))
                .isInstanceOf(ResourceAccessException.class);
        assertThatThrownBy(() -> sender.send("http://notification-service", request()))
                .isInstanceOf(ResourceAccessException.class);

        assertThatThrownBy(() -> sender.send("http://notification-service", request()))
                .isInstanceOf(CallNotPermittedException.class);

        verify(restTemplate, times(2)).postForEntity(anyString(), any(), eq(Void.class));
    }

    private static SendTransactionalEmailRequest request() {
        return SendTransactionalEmailRequest.builder()
                .recipientEmail("test@example.com")
                .templateCode("EMAIL_VERIFICATION")
                .build();
    }
}
