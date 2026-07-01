package com.aisales.identity.infrastructure.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aisales.notification")
public class NotificationClientProperties {

    private boolean enabled = true;

    private String baseUrl = "http://localhost:8090";

    /**
     * When notification-service is unavailable, log the email content locally (dev only).
     */
    private boolean fallbackLogging = true;
}
