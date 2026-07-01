package com.aisales.notification.application.service;

import com.aisales.notification.domain.enums.EmailTemplateCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailTemplateRenderer {

    private final NotificationProperties notificationProperties;

    public RenderedEmail render(EmailTemplateCode templateCode, Map<String, String> variables) {
        String firstName = variables.getOrDefault("firstName", "there");
        return switch (templateCode) {
            case EMAIL_VERIFICATION -> verificationEmail(firstName, variables);
            case PASSWORD_RESET -> passwordResetEmail(firstName, variables);
        };
    }

    private RenderedEmail verificationEmail(String firstName, Map<String, String> variables) {
        String token = variables.getOrDefault("token", "");
        String link = variables.getOrDefault("verificationLink", "");
        String subject = notificationProperties.getAppName() + " — verify your email";
        String body = """
                Hi %s,

                Thanks for registering with %s.

                Verify your email using this link:
                %s

                Or use this verification token in the API / Postman:
                %s

                This link expires in 24 hours.

                If you did not create an account, you can ignore this email.
                """.formatted(firstName, notificationProperties.getAppName(), link, token);
        return new RenderedEmail(subject, body.trim());
    }

    private RenderedEmail passwordResetEmail(String firstName, Map<String, String> variables) {
        String token = variables.getOrDefault("token", "");
        String link = variables.getOrDefault("resetLink", "");
        String subject = notificationProperties.getAppName() + " — reset your password";
        String body = """
                Hi %s,

                We received a request to reset your %s password.

                Reset your password using this link:
                %s

                Or use this reset token in the API / Postman:
                %s

                This link expires in 1 hour.

                If you did not request a reset, you can ignore this email.
                """.formatted(firstName, notificationProperties.getAppName(), link, token);
        return new RenderedEmail(subject, body.trim());
    }

    public record RenderedEmail(String subject, String body) {
    }
}
