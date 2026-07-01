package com.aisales.common.events.config;

import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.events.publisher.KafkaEventPublisher;
import com.aisales.common.events.publisher.LoggingEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public EventPublisher loggingEventPublisher() {
        return new LoggingEventPublisher();
    }

    @Bean
    @ConditionalOnProperty(name = "aisales.events.publisher", havingValue = "kafka", matchIfMissing = true)
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean(EventPublisher.class)
    public EventPublisher kafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                              ObjectMapper objectMapper) {
        return new KafkaEventPublisher(kafkaTemplate, objectMapper);
    }
}
