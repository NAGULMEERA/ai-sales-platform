package com.aisales.marketplace.infrastructure.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aisales.platform")
public class PlatformVersionProperties {

    /** Running platform semver used for plugin compatibility checks. */
    private String version = "1.0.0";
}
