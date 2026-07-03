package com.aisales.common.observability.resilience;

import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Structured logging for every Resilience4j {@link Retry} instance configured anywhere on the
 * platform so every service gets consistent retry observability for free instead of duplicating
 * listener wiring per outbound call site.
 *
 * <p>Per-instance Micrometer metrics ({@code resilience4j.retry.calls} with a {@code kind} tag of
 * successful_without_retry / successful_with_retry / failed_with_retry / failed_without_retry) are
 * already auto-registered by {@code resilience4j-micrometer} on the classpath; this class adds the
 * complementary structured log line for each individual retry attempt, which aggregate metrics
 * alone cannot provide (Rule 08).
 *
 * <p>Resilience4j 2.3+ composes all {@link RegistryEventConsumer} beans into the primary
 * {@code retryRegistryEventConsumer}; this bean must therefore use a distinct name.
 */
@Slf4j
@Configuration
public class RetryObservabilityConfig {

    @Bean
    public RegistryEventConsumer<Retry> platformRetryLoggingEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> event) {
                attachLogging(event.getAddedEntry());
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> event) {
                // No-op: retry instances are static platform configuration, never removed at runtime.
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> event) {
                attachLogging(event.getNewEntry());
            }
        };
    }

    private void attachLogging(Retry retry) {
        String targetService = retry.getName();
        retry.getEventPublisher()
                .onRetry(event -> log.warn("Retrying outbound call after transient failure {} {} {} {}",
                        StructuredArguments.kv("target_service", targetService),
                        StructuredArguments.kv("retry_attempt", event.getNumberOfRetryAttempts()),
                        StructuredArguments.kv("wait_ms", event.getWaitInterval().toMillis()),
                        StructuredArguments.kv("error", messageOf(event.getLastThrowable()))))
                .onSuccess(event -> {
                    if (event.getNumberOfRetryAttempts() > 0) {
                        log.info("Outbound call succeeded after retrying {} {}",
                                StructuredArguments.kv("target_service", targetService),
                                StructuredArguments.kv("retry_attempts", event.getNumberOfRetryAttempts()));
                    }
                })
                .onError(event -> log.error("Outbound call failed after exhausting retries {} {} {}",
                        StructuredArguments.kv("target_service", targetService),
                        StructuredArguments.kv("retry_attempts", event.getNumberOfRetryAttempts()),
                        StructuredArguments.kv("error", messageOf(event.getLastThrowable()))));
    }

    private static String messageOf(Throwable throwable) {
        return throwable == null ? null : throwable.getMessage();
    }
}
