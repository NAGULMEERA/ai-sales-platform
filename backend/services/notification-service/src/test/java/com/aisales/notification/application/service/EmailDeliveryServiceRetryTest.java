package com.aisales.notification.application.service;

import com.aisales.common.contracts.notification.SendTransactionalEmailRequest;
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
import org.springframework.mail.MailAuthenticationException;
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

/**
 * Verifies the {@code smtpEmail} Resilience4j retry policy end-to-end: a real {@code @Retry}
 * -advised {@link EmailDeliveryService} bean in a minimal Spring context, backed by a mocked
 * {@link JavaMailSender}. Mirrors {@code NotificationHttpSenderRetryTest} but exercises the
 * {@code MailException} branch of {@link com.aisales.common.observability.http.TransientFailurePredicate}
 * instead of the HTTP branch.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        EmailDeliveryService.class,
        EmailTemplateRenderer.class,
        EmailDeliveryServiceRetryTest.TestConfig.class
})
@ImportAutoConfiguration({AopAutoConfiguration.class, RetryAutoConfiguration.class})
@TestPropertySource(properties = {
        "resilience4j.retry.instances.smtpEmail.max-attempts=2",
        "resilience4j.retry.instances.smtpEmail.wait-duration=5ms",
        "resilience4j.retry.instances.smtpEmail.retry-exception-predicate="
                + "com.aisales.common.observability.http.TransientFailurePredicate"
})
class EmailDeliveryServiceRetryTest {

    @Autowired
    private EmailDeliveryService emailDeliveryService;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void shouldRetryOnceOnTransientSmtpFailureThenSucceed() {
        doThrow(new MailSendException("connection reset", new ConnectException()))
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        emailDeliveryService.sendTransactionalEmail(request());

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldNotRetryAuthenticationFailure() {
        doThrow(new MailAuthenticationException("bad credentials"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailDeliveryService.sendTransactionalEmail(request()))
                .isInstanceOf(MailAuthenticationException.class);

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
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
