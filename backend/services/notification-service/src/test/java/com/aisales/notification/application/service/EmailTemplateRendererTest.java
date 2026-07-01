package com.aisales.notification.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateRendererTest {

    private EmailTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = new NotificationProperties();
        properties.setAppName("AI Sales Platform");
        renderer = new EmailTemplateRenderer(properties);
    }

    @Test
    void shouldRenderVerificationEmailWithTokenAndLink() {
        var rendered = renderer.render(
                com.aisales.notification.domain.enums.EmailTemplateCode.EMAIL_VERIFICATION,
                Map.of(
                        "firstName", "Jane",
                        "token", "abc-123",
                        "verificationLink", "http://localhost:8081/api/v1/auth/verify-email?token=abc-123"));

        assertThat(rendered.subject()).contains("verify your email");
        assertThat(rendered.body()).contains("Jane");
        assertThat(rendered.body()).contains("abc-123");
        assertThat(rendered.body()).contains("http://localhost:8081/api/v1/auth/verify-email?token=abc-123");
    }

    @Test
    void shouldRenderPasswordResetEmailWithTokenAndLink() {
        var rendered = renderer.render(
                com.aisales.notification.domain.enums.EmailTemplateCode.PASSWORD_RESET,
                Map.of(
                        "firstName", "John",
                        "token", "reset-456",
                        "resetLink", "http://localhost:3000/reset-password?token=reset-456"));

        assertThat(rendered.subject()).contains("reset your password");
        assertThat(rendered.body()).contains("John");
        assertThat(rendered.body()).contains("reset-456");
    }
}
