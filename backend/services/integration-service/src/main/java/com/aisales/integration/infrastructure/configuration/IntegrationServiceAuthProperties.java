package com.aisales.integration.infrastructure.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bearer access token used for outbound Feign calls to Lead Service from webhooks
 * (no end-user JWT on the Meta callback).
 */
@Data
@ConfigurationProperties(prefix = "aisales.integration.service-auth")
public class IntegrationServiceAuthProperties {

    /** Platform access JWT for the target tenant (or service account). */
    private String bearerToken = "";
}
