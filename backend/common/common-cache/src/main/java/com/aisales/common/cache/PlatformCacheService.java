package com.aisales.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.concurrent.locks.ReentrantLock;

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
    private final PlatformCacheMetrics metrics;
    private final ConcurrentMap<String, ReentrantLock> loadLocks = new ConcurrentHashMap<>();

    public <T> Optional<T> get(String namespace, String key, Class<T> type) {
        return get(namespace, key, type, true);
    }

    private <T> Optional<T> get(String namespace, String key, Class<T> type, boolean recordMetrics) {
        String redisKey = keyGenerator.key(namespace, key);
        String raw = metrics.recordLatency("get", namespace, () -> redisTemplate.opsForValue().get(redisKey));
        if (raw == null) {
            if (recordMetrics) {
                metrics.recordMiss(namespace);
            }
            return Optional.empty();
        }
        try {
            if (recordMetrics) {
                metrics.recordHit(namespace);
            }
            return Optional.of(objectMapper.readValue(raw, type));
        } catch (JsonProcessingException ex) {
            log.warn("cache_deserialize_failed namespace={} key={}", namespace, key, ex);
            redisTemplate.delete(redisKey);
            metrics.recordLoadFailure(namespace);
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
        if (!properties.isStampedeProtectionEnabled()) {
            return loadAndCache(namespace, key, loader, ttl);
        }
        return getOrLoadWithStampedeProtection(namespace, key, type, loader, ttl);
    }

    private <T> T getOrLoadWithStampedeProtection(String namespace, String key, Class<T> type,
                                                  Supplier<T> loader, Duration ttl) {
        String redisKey = keyGenerator.key(namespace, key);
        ReentrantLock lock = loadLocks.computeIfAbsent(redisKey, ignored -> new ReentrantLock());
        boolean acquired = false;
        try {
            acquired = lock.tryLock(properties.getStampedeLockTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("cache_stampede_lock_timeout namespace={} key={}", namespace, key);
                return loadAndCache(namespace, key, loader, ttl);
            }
            Optional<T> cached = get(namespace, key, type, false);
            return cached.orElseGet(() -> loadAndCache(namespace, key, loader, ttl));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            metrics.recordLoadFailure(namespace);
            return loadAndCache(namespace, key, loader, ttl);
        } finally {
            if (acquired) {
                lock.unlock();
            }
            if (!lock.isLocked() && !lock.hasQueuedThreads()) {
                loadLocks.remove(redisKey, lock);
            }
        }
    }

    private <T> T loadAndCache(String namespace, String key, Supplier<T> loader, Duration ttl) {
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
            metrics.recordLatency("put", namespace, () -> {
                redisTemplate.opsForValue().set(redisKey, json, ttl);
                return null;
            });
            metrics.recordPut(namespace);
        } catch (JsonProcessingException ex) {
            log.warn("cache_serialize_failed namespace={} key={}", namespace, key, ex);
            metrics.recordLoadFailure(namespace);
        }
    }

    public void evict(String namespace, String key) {
        String redisKey = keyGenerator.key(namespace, key);
        Boolean deleted = metrics.recordLatency("evict", namespace, () -> redisTemplate.delete(redisKey));
        if (Boolean.TRUE.equals(deleted)) {
            metrics.recordEviction(namespace, 1);
        }
    }

    public void invalidate(String namespace, String key) {
        evict(namespace, key);
    }

    public void evictNamespace(String namespace) {
        evictNamespaceAndCount(namespace);
    }

    public void invalidateNamespace(String namespace) {
        evictNamespace(namespace);
    }

    public long evictNamespaceAndCount(String namespace) {
        String pattern = keyGenerator.namespacePattern(namespace);
        long deleted = evictByPattern(namespace, pattern);
        metrics.recordEviction(namespace, deleted);
        return deleted;
    }

    public long evictTenantAndCount() {
        String pattern = keyGenerator.tenantPattern();
        long deleted = evictByPattern("*", pattern);
        metrics.recordEviction("*", deleted);
        return deleted;
    }

    public void invalidateTenant() {
        evictTenantAndCount();
    }

    private long evictByPattern(String namespace, String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        Long deleted = metrics.recordLatency("evict", namespace, () -> redisTemplate.delete(keys));
        return Optional.ofNullable(deleted).orElse(0L);
    }

    Set<String> keysForNamespace(String namespace) {
        Set<String> keys = redisTemplate.keys(keyGenerator.namespacePattern(namespace));
        return keys != null ? keys : Collections.emptySet();
    }
}
