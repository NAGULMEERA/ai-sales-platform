package com.aisales.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-endpoint Redis token-bucket limits for public authentication routes (Rule 06: rate limit
 * login, registration, and password reset). Limits are keyed by client IP at the gateway edge.
 */
@Data
@ConfigurationProperties(prefix = "aisales.gateway.rate-limit")
public class GatewayRateLimitProperties {

    /** When false, auth routes are forwarded without a {@code RequestRateLimiter} filter. */
    private boolean enabled = true;

    private Limit auth = new Limit(5, 10);
    private Limit passwordReset = new Limit(2, 5);

    @Data
    public static class Limit {
        /** Steady-state requests allowed per second (Redis token bucket replenish rate). */
        private int replenishRate;
        /** Maximum burst size in a single window. */
        private int burstCapacity;

        public Limit() {
        }

        public Limit(int replenishRate, int burstCapacity) {
            this.replenishRate = replenishRate;
            this.burstCapacity = burstCapacity;
        }
    }
}
