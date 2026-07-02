package com.aisales.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(name = "aisales.cache.enabled", havingValue = "true")
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {

    @Bean
    public TenantCacheKeyGenerator tenantCacheKeyGenerator(CacheProperties properties) {
        return new TenantCacheKeyGenerator(properties);
    }

    @Bean
    public StringRedisTemplate platformStringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public PlatformCacheService platformCacheService(
            StringRedisTemplate redisTemplate,
            TenantCacheKeyGenerator keyGenerator,
            CacheProperties properties,
            ObjectMapper objectMapper) {
        return new PlatformCacheService(redisTemplate, keyGenerator, properties, objectMapper);
    }
}
