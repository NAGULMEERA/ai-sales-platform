package com.aisales.common.events.config;

import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.events.publisher.KafkaEventPublisher;
import com.aisales.common.events.publisher.LoggingEventPublisher;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@ConditionalOnClass(KafkaTemplate.class)
public class EventsAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "aisales.events.publisher", havingValue = "logging")
    @ConditionalOnMissingBean(EventPublisher.class)
    public EventPublisher loggingEventPublisher(ObjectProvider<PlatformMetrics> platformMetrics) {
        return new LoggingEventPublisher(platformMetrics.getIfAvailable());
    }

    @Configuration
    @ConditionalOnProperty(name = "aisales.events.outbox.enabled", havingValue = "false", matchIfMissing = true)
    static class DirectKafkaPublisherConfiguration {

        @Bean
        @ConditionalOnProperty(name = "aisales.events.publisher", havingValue = "kafka", matchIfMissing = true)
        @ConditionalOnBean(KafkaTemplate.class)
        @ConditionalOnMissingBean(EventPublisher.class)
        public EventPublisher kafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                                  ObjectMapper objectMapper,
                                                  ObjectProvider<PlatformMetrics> platformMetrics) {
            return new KafkaEventPublisher(kafkaTemplate, objectMapper, platformMetrics.getIfAvailable());
        }
    }
}
