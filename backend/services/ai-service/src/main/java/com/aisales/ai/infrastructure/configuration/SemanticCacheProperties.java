package com.aisales.ai.infrastructure.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "aisales.ai.semantic-cache")
public class SemanticCacheProperties {

    private boolean enabled = true;

    /** Max cosine distance for a semantic hit (0 = identical, 2 = opposite). */
    private double maxCosineDistance = 0.1;

    private Duration defaultTtl = Duration.ofDays(7);

    private Duration redisTtl = Duration.ofHours(1);
}
