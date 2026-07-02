package com.aisales.common.observability.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "aisales.observability")
public class ObservabilityProperties {

    /**
     * Deployment environment tag applied to all metrics (local, dev, staging, prod).
     */
    private String environment = "local";
}
