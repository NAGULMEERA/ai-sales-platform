package com.aisales.notification.infrastructure.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Properties;

@Configuration
public class MailConfiguration {

    /**
     * Spring Boot's {@code MailSenderAutoConfiguration} is gated by
     * {@code @ConditionalOnMissingBean(MailSender.class)}. We supply our own
     * {@link JavaMailSender} beans below, so {@link MailProperties} must be bound
     * independently or the cycle described in earlier revisions returns.
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.mail")
    public MailProperties mailProperties() {
        return new MailProperties();
    }

    /**
     * Used only when {@code aisales.notification.delivery-mode=log} (safe default), where
     * {@link com.aisales.notification.application.service.EmailDeliveryService} never invokes
     * {@code send(...)}.
     */
    @Bean
    @ConditionalOnProperty(name = "aisales.notification.delivery-mode", havingValue = "log", matchIfMissing = true)
    public JavaMailSender loggingJavaMailSender(MailProperties mailProperties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("localhost");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.putAll(mailProperties.getProperties());
        return sender;
    }

    /**
     * Real SMTP sender for Mailpit / Gmail / any SMTP provider when
     * {@code aisales.notification.delivery-mode=smtp}.
     */
    @Bean
    @ConditionalOnProperty(name = "aisales.notification.delivery-mode", havingValue = "smtp")
    public JavaMailSender smtpJavaMailSender(MailProperties mailProperties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        applyProperties(mailProperties, sender);
        return sender;
    }

    private static void applyProperties(MailProperties properties, JavaMailSenderImpl sender) {
        sender.setHost(properties.getHost());
        if (properties.getPort() != null) {
            sender.setPort(properties.getPort());
        }
        if (StringUtils.hasText(properties.getUsername())) {
            sender.setUsername(properties.getUsername());
        }
        if (StringUtils.hasText(properties.getPassword())) {
            sender.setPassword(properties.getPassword());
        }
        sender.setProtocol(properties.getProtocol());
        if (properties.getDefaultEncoding() != null) {
            sender.setDefaultEncoding(properties.getDefaultEncoding().name());
        }
        Properties javaMailProperties = sender.getJavaMailProperties();
        Map<String, String> configured = properties.getProperties();
        if (configured != null && !configured.isEmpty()) {
            javaMailProperties.putAll(configured);
        }
    }
}
