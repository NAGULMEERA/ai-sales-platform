package com.aisales.gateway.ratelimit;

import java.util.Map;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import reactor.core.publisher.Mono;

/**
 * Delegates to FREE or PREMIUM {@link RedisRateLimiter} buckets based on a key prefix
 * produced by {@link TenantPlanKeyResolver} ({@code PREMIUM:…} vs {@code FREE:…}).
 */
public class PlanTierRateLimiter implements RateLimiter<RedisRateLimiter.Config> {

    public static final String PREMIUM_PREFIX = "PREMIUM:";
    public static final String FREE_PREFIX = "FREE:";

    private final RedisRateLimiter freeLimiter;
    private final RedisRateLimiter paidLimiter;

    public PlanTierRateLimiter(RedisRateLimiter freeLimiter, RedisRateLimiter paidLimiter) {
        this.freeLimiter = freeLimiter;
        this.paidLimiter = paidLimiter;
    }

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        if (id != null && id.startsWith(PREMIUM_PREFIX)) {
            return paidLimiter.isAllowed(routeId, id.substring(PREMIUM_PREFIX.length()));
        }
        String key = id != null && id.startsWith(FREE_PREFIX)
                ? id.substring(FREE_PREFIX.length())
                : id;
        return freeLimiter.isAllowed(routeId, key);
    }

    @Override
    public Map<String, RedisRateLimiter.Config> getConfig() {
        return freeLimiter.getConfig();
    }

    @Override
    public Class<RedisRateLimiter.Config> getConfigClass() {
        return RedisRateLimiter.Config.class;
    }

    @Override
    public RedisRateLimiter.Config newConfig() {
        return freeLimiter.newConfig();
    }
}
