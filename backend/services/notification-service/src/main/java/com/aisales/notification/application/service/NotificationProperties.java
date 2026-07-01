package com.aisales.notification.application.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aisales.notification")
public class NotificationProperties {

    /**
     * log — write email to application logs (local dev).
     * smtp — send via Spring JavaMailSender.
     */
    private String deliveryMode = "log";

    private String fromAddress = "noreply@aisales.local";

    private String appName = "AI Sales Platform";
}
