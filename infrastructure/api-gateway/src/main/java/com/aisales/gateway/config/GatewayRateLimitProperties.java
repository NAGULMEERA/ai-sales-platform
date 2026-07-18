package com.aisales.gateway.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-endpoint Redis token-bucket limits at the gateway edge (Rule 06).
 */
@Data
@ConfigurationProperties(prefix = "aisales.gateway.rate-limit")
public class GatewayRateLimitProperties {

    /** When false, rate-limited routes are forwarded without a {@code RequestRateLimiter} filter. */
    private boolean enabled = true;

    /**
     * CIDRs of trusted reverse proxies / load balancers. {@code X-Forwarded-For} is ignored unless
     * the direct peer matches one of these. Empty (default) = never trust XFF.
     */
    private List<String> trustedProxyCidrs = new ArrayList<>();

    private Limit auth = new Limit(5, 10);
    private Limit passwordReset = new Limit(2, 5);
    private Limit aiExecute = new Limit(20, 40);

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
