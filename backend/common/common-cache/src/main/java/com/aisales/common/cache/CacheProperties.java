package com.aisales.common.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "aisales.cache")
public class CacheProperties {

    /** When false, {@link PlatformCacheService} is not registered. */
    private boolean enabled = false;

    /** Prefix applied to every cache key (before tenant segment). */
    private String keyPrefix = "aisales";

    /** Default TTL for {@link PlatformCacheService#getOrLoad} when not overridden. */
    private Duration defaultTtl = Duration.ofMinutes(5);
}
