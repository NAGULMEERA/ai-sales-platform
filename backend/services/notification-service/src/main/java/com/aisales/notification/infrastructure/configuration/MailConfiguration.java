package com.aisales.notification.infrastructure.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfiguration {

    /**
     * Spring Boot's {@code MailSenderAutoConfiguration} (the class that normally supplies the
     * {@link MailProperties} bean via {@code @EnableConfigurationProperties}) is itself gated by
     * {@code @ConditionalOnMissingBean(MailSender.class)} on the whole auto-configuration class,
     * not just its bean methods. Since {@link #loggingJavaMailSender} below is our own
     * {@code JavaMailSender} (a {@code MailSender}) bean, declaring it would silently disable that
     * entire auto-configuration - including {@code MailProperties} - creating a circular
     * dependency the moment {@code loggingJavaMailSender} tries to inject it. Binding
     * {@code spring.mail.*} independently here breaks that cycle.
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.mail")
    public MailProperties mailProperties() {
        return new MailProperties();
    }

    /**
     * Used only when {@code aisales.notification.delivery-mode=log} (local/dev default), where
     * {@link com.aisales.notification.application.service.EmailDeliveryService} never actually
     * invokes {@code send(...)}. When delivery-mode is {@code smtp}, Spring Boot's
     * auto-configured {@link JavaMailSender} (built from {@code spring.mail.*}, including the
     * SMTP connect/read/write timeouts) is used instead - see {@link #mailProperties()} for why
     * that auto-configuration still works correctly despite this bean also implementing
     * {@code MailSender}.
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
}
