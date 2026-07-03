package com.aisales.identity.infrastructure.client;

import com.aisales.common.contracts.notification.SendTransactionalEmailRequest;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
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
 * Verifies the {@code notificationService} Resilience4j retry policy end-to-end: a real {@code
 * @Retry}-advised bean in a real (minimal) Spring context, backed by a mocked {@link
 * RestTemplate}. Only {@link AopAutoConfiguration} and {@link RetryAutoConfiguration} are
 * imported (rather than {@code @SpringBootTest}'s full auto-configuration scan) so this test
 * stays fast and infrastructure-free (Rule 07: unit/component tests must not require a database,
 * Kafka, or other external dependencies).
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NotificationHttpSender.class)
@ImportAutoConfiguration({AopAutoConfiguration.class, RetryAutoConfiguration.class})
@TestPropertySource(properties = {
        "resilience4j.retry.instances.notificationService.max-attempts=3",
        "resilience4j.retry.instances.notificationService.wait-duration=5ms",
        "resilience4j.retry.instances.notificationService.retry-exception-predicate="
                + "com.aisales.common.observability.http.TransientFailurePredicate"
})
class NotificationHttpSenderRetryTest {

    @Autowired
    private NotificationHttpSender sender;

    @MockitoBean
    private RestTemplate restTemplate;

    @MockitoBean
    private InternalServiceTokenProvider internalServiceTokenProvider;

    @BeforeEach
    void stubServiceToken() {
        when(internalServiceTokenProvider.mintServiceToken()).thenReturn("test-service-token");
    }

    @Test
    void shouldRetryTransientFailuresUntilSuccess() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenThrow(new ResourceAccessException("connection reset"))
                .thenThrow(new ResourceAccessException("connection reset"))
                .thenReturn(ResponseEntity.ok().build());

        sender.send("http://notification-service", request());

        verify(restTemplate, times(3)).postForEntity(anyString(), any(), eq(Void.class));
    }

    @Test
    void shouldGiveUpAfterExhaustingConfiguredAttempts() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenThrow(new ResourceAccessException("connection reset"));

        assertThatThrownBy(() -> sender.send("http://notification-service", request()))
                .isInstanceOf(ResourceAccessException.class);

        verify(restTemplate, times(3)).postForEntity(anyString(), any(), eq(Void.class));
    }

    @Test
    void shouldNotRetryNonTransientClientErrors() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST, "Bad Request", null, null, null));

        assertThatThrownBy(() -> sender.send("http://notification-service", request()))
                .isInstanceOf(HttpClientErrorException.class);

        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Void.class));
    }

    private static SendTransactionalEmailRequest request() {
        return SendTransactionalEmailRequest.builder()
                .recipientEmail("test@example.com")
                .templateCode("EMAIL_VERIFICATION")
                .build();
    }
}
