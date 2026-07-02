package com.aisales.identity.infrastructure.configuration;

import com.aisales.common.observability.http.CorrelationIdPropagationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfiguration {

    /**
     * Outbound calls to other services (currently only notification-service) must never block
     * indefinitely. Timeouts are configurable but always sensible-by-default (Rule 08/09
     * resilience: timeouts are the highest-priority resilience control).
     *
     * <p>This client only ever calls services on the same internal network (same Docker
     * network / cluster), never the public internet, so both timeouts are intentionally tighter
     * than the ones used for internet-facing calls (e.g. Google OAuth2, OpenAI): a healthy
     * internal dependency should complete in well under a second, so 2s/5s already gives
     * generous headroom for GC pauses or a cold JVM without letting a stuck downstream service
     * exhaust the caller's thread pool.
     *
     * <p>{@link CorrelationIdPropagationInterceptor} forwards this request's {@code correlation_id}
     * onto notification-service so the two services' logs can be correlated by the same id (Rule
     * 08: "never lose traceability"); trace/span id propagation is already handled automatically
     * by Micrometer Tracing's instrumentation of {@link RestTemplateBuilder}.
     */
    @Bean
    public RestTemplate restTemplate(
            RestTemplateBuilder builder,
            @Value("${aisales.http.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${aisales.http.read-timeout-ms:5000}") long readTimeoutMs) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .additionalInterceptors(new CorrelationIdPropagationInterceptor())
                .build();
    }
}
