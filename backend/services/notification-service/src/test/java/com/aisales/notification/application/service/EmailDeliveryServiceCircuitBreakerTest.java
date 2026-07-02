package com.aisales.notification.application.service;

import com.aisales.common.contracts.notification.SendTransactionalEmailRequest;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.ConnectException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        EmailDeliveryService.class,
        EmailTemplateRenderer.class,
        EmailDeliveryServiceCircuitBreakerTest.TestConfig.class
})
@ImportAutoConfiguration({
        AopAutoConfiguration.class,
        RetryAutoConfiguration.class,
        CircuitBreakerAutoConfiguration.class
})
@TestPropertySource(properties = {
        "resilience4j.retry.instances.smtpEmail.max-attempts=1",
        "resilience4j.retry.instances.smtpEmail.retry-exception-predicate="
                + "com.aisales.common.observability.http.TransientFailurePredicate",
        "resilience4j.circuitbreaker.instances.smtpEmail.sliding-window-size=2",
        "resilience4j.circuitbreaker.instances.smtpEmail.minimum-number-of-calls=2",
        "resilience4j.circuitbreaker.instances.smtpEmail.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.smtpEmail.wait-duration-in-open-state=600s",
        "resilience4j.circuitbreaker.instances.smtpEmail.record-failure-predicate="
                + "com.aisales.common.observability.http.TransientFailurePredicate"
})
class EmailDeliveryServiceCircuitBreakerTest {

    @Autowired
    private EmailDeliveryService emailDeliveryService;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void shouldOpenCircuitAndFastFailAfterSustainedSmtpFailures() {
        doThrow(new MailSendException("connection reset", new ConnectException()))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailDeliveryService.sendTransactionalEmail(request()))
                .isInstanceOf(MailSendException.class);
        assertThatThrownBy(() -> emailDeliveryService.sendTransactionalEmail(request()))
                .isInstanceOf(MailSendException.class);

        assertThatThrownBy(() -> emailDeliveryService.sendTransactionalEmail(request()))
                .isInstanceOf(CallNotPermittedException.class);

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    private static SendTransactionalEmailRequest request() {
        return SendTransactionalEmailRequest.builder()
                .recipientEmail("test@example.com")
                .templateCode("EMAIL_VERIFICATION")
                .variables(Map.of("firstName", "Ada", "token", "tok", "verificationLink", "http://x"))
                .build();
    }

    @Configuration
    static class TestConfig {

        @Bean
        MailProperties mailProperties() {
            return new MailProperties();
        }

        @Bean
        NotificationProperties notificationProperties() {
            NotificationProperties properties = new NotificationProperties();
            properties.setDeliveryMode("smtp");
            properties.setFromAddress("noreply@aisales.local");
            properties.setAppName("AI Sales Platform");
            return properties;
        }
    }
}
