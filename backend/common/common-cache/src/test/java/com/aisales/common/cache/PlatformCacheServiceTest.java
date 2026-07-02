package com.aisales.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PlatformCacheService cacheService;

    @BeforeEach
    void setUp() {
        CacheProperties properties = new CacheProperties();
        properties.setDefaultTtl(Duration.ofMinutes(5));
        TenantCacheKeyGenerator keyGenerator = new TenantCacheKeyGenerator(properties);
        cacheService = new PlatformCacheService(redisTemplate, keyGenerator, properties, new ObjectMapper());
    }

    @Test
    void shouldLoadOnCacheMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UUID id = UUID.randomUUID();
        when(valueOperations.get(any())).thenReturn(null);

        String loaded = cacheService.getOrLoad("item", id.toString(), String.class, () -> "loaded");

        assertThat(loaded).isEqualTo("loaded");
        verify(valueOperations).set(any(), eq("\"loaded\""), eq(Duration.ofMinutes(5)));
    }

    @Test
    void shouldReturnCachedValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UUID id = UUID.randomUUID();
        when(valueOperations.get(any())).thenReturn("\"cached\"");

        Optional<String> result = cacheService.get("item", id.toString(), String.class);

        assertThat(result).contains("cached");
    }

    @Test
    void shouldEvictKey() {
        UUID id = UUID.randomUUID();
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        cacheService.evict("item", id.toString());

        verify(redisTemplate).delete(keyCaptor.capture());
        assertThat(keyCaptor.getValue()).contains(":platform:item:" + id);
    }
}
