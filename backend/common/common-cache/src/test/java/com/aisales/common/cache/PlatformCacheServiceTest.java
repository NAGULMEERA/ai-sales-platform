package com.aisales.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PlatformCacheService cacheService;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        CacheProperties properties = new CacheProperties();
        properties.setDefaultTtl(Duration.ofMinutes(5));
        TenantCacheKeyGenerator keyGenerator = new TenantCacheKeyGenerator(properties);
        PlatformCacheMetrics metrics = new PlatformCacheMetrics(new SimpleMeterRegistry());
        cacheService = new PlatformCacheService(redisTemplate, keyGenerator, properties, new ObjectMapper(), metrics);
        executorService = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
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

    @Test
    void shouldEvictNamespaceAndReturnDeletedCount() {
        when(redisTemplate.keys("aisales:platform:item:*")).thenReturn(Set.of(
                "aisales:platform:item:1", "aisales:platform:item:2"));
        when(redisTemplate.delete(anyCollection())).thenReturn(2L);

        long deleted = cacheService.evictNamespaceAndCount("item");

        assertThat(deleted).isEqualTo(2);
        verify(redisTemplate).delete(Set.of("aisales:platform:item:1", "aisales:platform:item:2"));
    }

    @Test
    void shouldProtectConcurrentCacheMissesFromStampede() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        AtomicReference<String> cachedValue = new AtomicReference<>();
        AtomicInteger loaderCalls = new AtomicInteger();
        CountDownLatch loaderStarted = new CountDownLatch(1);

        when(valueOperations.get(any())).thenAnswer(invocation -> cachedValue.get());
        doAnswer(invocation -> {
            cachedValue.set(invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(any(), any(), any(Duration.class));

        Callable<String> cacheCall = () -> cacheService.getOrLoad("item", "expensive", String.class, () -> {
            loaderCalls.incrementAndGet();
            loaderStarted.countDown();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return "loaded";
        });

        Future<String> first = executorService.submit(cacheCall);
        loaderStarted.await();
        Future<String> second = executorService.submit(cacheCall);

        assertThat(first.get()).isEqualTo("loaded");
        assertThat(second.get()).isEqualTo("loaded");
        assertThat(loaderCalls.get()).isEqualTo(1);
    }
}
