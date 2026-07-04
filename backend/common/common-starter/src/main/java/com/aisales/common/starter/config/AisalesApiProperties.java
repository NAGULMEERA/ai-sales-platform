package com.aisales.common.starter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Service-level OpenAPI metadata overrides. Platform defaults (JWT security scheme, contact)
 * are applied by {@link OpenApiAutoConfiguration}; services customize only title/description/version.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "aisales.api")
public class AisalesApiProperties {

    /** When blank, {@code spring.application.name} is used as the API title. */
    private String title;

    private String description = "AI Sales Platform REST API";

    private String version = "1.0.0";

    private String contactName = "AI Sales Team";

    private String contactEmail = "support@aisales.io";
}
