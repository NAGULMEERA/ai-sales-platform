package com.aisales.common.observability.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "aisales.observability")
public class ObservabilityProperties {

    /**
     * Deployment environment tag applied to all metrics (local, dev, staging, prod).
     */
    private String environment = "local";

    /** Requests slower than this threshold are logged as WARN and counted separately. */
    private Duration slowRequestThreshold = Duration.ofSeconds(1);

    /** Outbound calls slower than this threshold are classified as slow calls. */
    private Duration slowOutboundCallThreshold = Duration.ofSeconds(2);
}
