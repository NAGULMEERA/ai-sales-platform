package com.aisales.gateway.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-endpoint Redis token-bucket limits at the gateway edge (Rule 06).
 * Authenticated routes use tenant+plan keys; public auth/webhook routes use client IP.
 */
@Data
@ConfigurationProperties(prefix = "aisales.gateway.rate-limit")
public class GatewayRateLimitProperties {

    /** When false, rate-limited routes are forwarded without a {@code RequestRateLimiter} filter. */
    private boolean enabled = true;

    /**
     * Seconds advertised in {@code Retry-After} when a request is rejected with HTTP 429.
     */
    private int retryAfterSeconds = 1;

    /**
     * CIDRs of trusted reverse proxies / load balancers. {@code X-Forwarded-For} is ignored unless
     * the direct peer matches one of these. Empty (default) = never trust XFF.
     */
    private List<String> trustedProxyCidrs = new ArrayList<>();

    private Limit auth = new Limit(5, 10);
    private Limit passwordReset = new Limit(2, 5);
    private Limit webhook = new Limit(50, 100);
    private Limit write = new Limit(30, 60);
    private Limit mediaUpload = new Limit(10, 20);
    private Limit search = new Limit(40, 80);

    /** AI execute limits for FREE-tier tenants. */
    private Limit aiExecute = new Limit(10, 20);
    /** AI execute limits for PREMIUM / paid tenants. */
    private Limit aiExecutePaid = new Limit(50, 100);

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
