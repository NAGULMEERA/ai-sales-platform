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

    /** When true, concurrent read-through misses for the same key share one loader execution per JVM. */
    private boolean stampedeProtectionEnabled = true;

    /** Maximum time a request waits for another request loading the same key before loading directly. */
    private Duration stampedeLockTimeout = Duration.ofSeconds(5);
}
