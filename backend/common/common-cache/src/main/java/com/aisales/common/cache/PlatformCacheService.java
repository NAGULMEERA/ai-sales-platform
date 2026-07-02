package com.aisales.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Tenant-aware read-through cache backed by Redis. Keys are generated via {@link TenantCacheKeyGenerator}.
 */
@Slf4j
@RequiredArgsConstructor
public class PlatformCacheService {

    private final StringRedisTemplate redisTemplate;
    private final TenantCacheKeyGenerator keyGenerator;
    private final CacheProperties properties;
    private final ObjectMapper objectMapper;

    public <T> Optional<T> get(String namespace, String key, Class<T> type) {
        String redisKey = keyGenerator.key(namespace, key);
        String raw = redisTemplate.opsForValue().get(redisKey);
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(raw, type));
        } catch (JsonProcessingException ex) {
            log.warn("cache_deserialize_failed namespace={} key={}", namespace, key, ex);
            redisTemplate.delete(redisKey);
            return Optional.empty();
        }
    }

    public <T> T getOrLoad(String namespace, String key, Class<T> type, Supplier<T> loader) {
        return getOrLoad(namespace, key, type, loader, properties.getDefaultTtl());
    }

    public <T> T getOrLoad(String namespace, String key, Class<T> type, Supplier<T> loader, Duration ttl) {
        Optional<T> cached = get(namespace, key, type);
        if (cached.isPresent()) {
            return cached.get();
        }
        T loaded = loader.get();
        if (loaded != null) {
            put(namespace, key, loaded, ttl);
        }
        return loaded;
    }

    public void put(String namespace, String key, Object value) {
        put(namespace, key, value, properties.getDefaultTtl());
    }

    public void put(String namespace, String key, Object value, Duration ttl) {
        if (value == null) {
            return;
        }
        String redisKey = keyGenerator.key(namespace, key);
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(redisKey, json, ttl);
        } catch (JsonProcessingException ex) {
            log.warn("cache_serialize_failed namespace={} key={}", namespace, key, ex);
        }
    }

    public void evict(String namespace, String key) {
        redisTemplate.delete(keyGenerator.key(namespace, key));
    }

    public void evictNamespace(String namespace) {
        String pattern = keyGenerator.namespacePattern(namespace);
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
