package com.aisales.gateway.config;

import com.aisales.gateway.ratelimit.PlanTierRateLimiter;
import com.aisales.gateway.support.ClientIpResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties(GatewayRateLimitProperties.class)
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver clientIpKeyResolver(ClientIpResolver clientIpResolver) {
        return exchange -> Mono.just(clientIpResolver.resolve(exchange));
    }

    @Bean("authRedisRateLimiter")
    @ConditionalOnProperty(name = "aisales.gateway.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public RedisRateLimiter authRedisRateLimiter(GatewayRateLimitProperties properties) {
        GatewayRateLimitProperties.Limit auth = properties.getAuth();
        return new RedisRateLimiter(auth.getReplenishRate(), auth.getBurstCapacity());
    }

    @Bean("passwordResetRedisRateLimiter")
    @ConditionalOnProperty(name = "aisales.gateway.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public RedisRateLimiter passwordResetRedisRateLimiter(GatewayRateLimitProperties properties) {
        GatewayRateLimitProperties.Limit passwordReset = properties.getPasswordReset();
        return new RedisRateLimiter(passwordReset.getReplenishRate(), passwordReset.getBurstCapacity());
    }

    @Bean("aiExecuteFreeRedisRateLimiter")
    @ConditionalOnProperty(name = "aisales.gateway.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public RedisRateLimiter aiExecuteFreeRedisRateLimiter(GatewayRateLimitProperties properties) {
        GatewayRateLimitProperties.Limit free = properties.getAiExecute();
        return new RedisRateLimiter(free.getReplenishRate(), free.getBurstCapacity());
    }

    @Bean("aiExecutePaidRedisRateLimiter")
    @ConditionalOnProperty(name = "aisales.gateway.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public RedisRateLimiter aiExecutePaidRedisRateLimiter(GatewayRateLimitProperties properties) {
        GatewayRateLimitProperties.Limit paid = properties.getAiExecutePaid();
        return new RedisRateLimiter(paid.getReplenishRate(), paid.getBurstCapacity());
    }

    @Bean("aiExecuteRedisRateLimiter")
    @ConditionalOnProperty(name = "aisales.gateway.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public PlanTierRateLimiter aiExecuteRedisRateLimiter(
            @Qualifier("aiExecuteFreeRedisRateLimiter") RedisRateLimiter free,
            @Qualifier("aiExecutePaidRedisRateLimiter") RedisRateLimiter paid) {
        return new PlanTierRateLimiter(free, paid);
    }

    @Bean("writeRedisRateLimiter")
    @ConditionalOnProperty(name = "aisales.gateway.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public RedisRateLimiter writeRedisRateLimiter(GatewayRateLimitProperties properties) {
        GatewayRateLimitProperties.Limit write = properties.getWrite();
        return new RedisRateLimiter(write.getReplenishRate(), write.getBurstCapacity());
    }

    @Bean("mediaUploadRedisRateLimiter")
    @ConditionalOnProperty(name = "aisales.gateway.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public RedisRateLimiter mediaUploadRedisRateLimiter(GatewayRateLimitProperties properties) {
        GatewayRateLimitProperties.Limit media = properties.getMediaUpload();
        return new RedisRateLimiter(media.getReplenishRate(), media.getBurstCapacity());
    }

    @Bean("searchRedisRateLimiter")
    @ConditionalOnProperty(name = "aisales.gateway.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public RedisRateLimiter searchRedisRateLimiter(GatewayRateLimitProperties properties) {
        GatewayRateLimitProperties.Limit search = properties.getSearch();
        return new RedisRateLimiter(search.getReplenishRate(), search.getBurstCapacity());
    }

    @Bean("webhookRedisRateLimiter")
    @ConditionalOnProperty(name = "aisales.gateway.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public RedisRateLimiter webhookRedisRateLimiter(GatewayRateLimitProperties properties) {
        GatewayRateLimitProperties.Limit webhook = properties.getWebhook();
        return new RedisRateLimiter(webhook.getReplenishRate(), webhook.getBurstCapacity());
    }
}
