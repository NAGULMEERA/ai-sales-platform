package com.aisales.gateway.ratelimit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class PlanTierRateLimiterTest {

    @Mock private RedisRateLimiter freeLimiter;
    @Mock private RedisRateLimiter paidLimiter;

    @Test
    void shouldDelegatePremiumKeysToPaidLimiter() {
        when(paidLimiter.isAllowed(eq("route"), eq("tenant-1")))
                .thenReturn(Mono.just(new RateLimiter.Response(true, java.util.Map.of())));
        PlanTierRateLimiter limiter = new PlanTierRateLimiter(freeLimiter, paidLimiter);

        limiter.isAllowed("route", "PREMIUM:tenant-1").block();

        verify(paidLimiter).isAllowed("route", "tenant-1");
    }

    @Test
    void shouldDelegateFreeKeysToFreeLimiter() {
        when(freeLimiter.isAllowed(eq("route"), eq("tenant-2")))
                .thenReturn(Mono.just(new RateLimiter.Response(true, java.util.Map.of())));
        PlanTierRateLimiter limiter = new PlanTierRateLimiter(freeLimiter, paidLimiter);

        limiter.isAllowed("route", "FREE:tenant-2").block();

        verify(freeLimiter).isAllowed("route", "tenant-2");
    }
}
