package com.aisales.common.observability.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Structured logging for every Resilience4j {@link CircuitBreaker} instance configured anywhere
 * on the platform. Micrometer metrics are auto-registered by {@code resilience4j-micrometer};
 * this adds the per-event log lines needed for production diagnosis (Rule 08).
 *
 * <p>Resilience4j 2.3+ composes all {@link RegistryEventConsumer} beans into the primary
 * {@code circuitBreakerRegistryEventConsumer}; this bean must therefore use a distinct name.
 */
@Slf4j
@Configuration
public class CircuitBreakerObservabilityConfig {

    @Bean
    public RegistryEventConsumer<CircuitBreaker> platformCircuitBreakerLoggingEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> event) {
                attachLogging(event.getAddedEntry());
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> event) {
                // No-op: circuit breaker instances are static platform configuration.
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> event) {
                attachLogging(event.getNewEntry());
            }
        };
    }

    private void attachLogging(CircuitBreaker circuitBreaker) {
        String targetService = circuitBreaker.getName();
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.warn("Circuit breaker state transition {} {} {}",
                        StructuredArguments.kv("target_service", targetService),
                        StructuredArguments.kv("from_state", event.getStateTransition().getFromState()),
                        StructuredArguments.kv("to_state", event.getStateTransition().getToState())))
                .onFailureRateExceeded(event -> log.warn("Circuit breaker failure rate exceeded {} {}",
                        StructuredArguments.kv("target_service", targetService),
                        StructuredArguments.kv("failure_rate", event.getFailureRate())))
                .onCallNotPermitted(event -> log.warn("Circuit breaker rejected call (OPEN) {}",
                        StructuredArguments.kv("target_service", targetService)))
                .onError(event -> log.debug("Circuit breaker recorded failure {} {}",
                        StructuredArguments.kv("target_service", targetService),
                        StructuredArguments.kv("error", messageOf(event.getThrowable()))));
    }

    private static String messageOf(Throwable throwable) {
        return throwable == null ? null : throwable.getMessage();
    }
}
