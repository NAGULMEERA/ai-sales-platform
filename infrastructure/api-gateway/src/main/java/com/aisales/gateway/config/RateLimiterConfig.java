package com.aisales.gateway.config;

import com.aisales.gateway.support.ClientIpResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties(GatewayRateLimitProperties.class)
public class RateLimiterConfig {

    @Bean
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

    @Bean("aiExecuteRedisRateLimiter")
    @ConditionalOnProperty(name = "aisales.gateway.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public RedisRateLimiter aiExecuteRedisRateLimiter(GatewayRateLimitProperties properties) {
        GatewayRateLimitProperties.Limit aiExecute = properties.getAiExecute();
        return new RedisRateLimiter(aiExecute.getReplenishRate(), aiExecute.getBurstCapacity());
    }
}
