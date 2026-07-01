package com.aisales.notification.infrastructure.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfiguration {

    @Bean
    @ConditionalOnProperty(name = "aisales.notification.delivery-mode", havingValue = "log", matchIfMissing = true)
    public JavaMailSender loggingJavaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("localhost");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        return sender;
    }
}
