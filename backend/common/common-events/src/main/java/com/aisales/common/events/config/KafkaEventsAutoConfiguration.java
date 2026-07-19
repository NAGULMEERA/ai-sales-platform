package com.aisales.common.events.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnBean(ConsumerFactory.class)
public class KafkaEventsAutoConfiguration {

    /**
     * RECORD ack only after successful listener return. Failures throw from
     * {@link com.aisales.common.events.consumer.IntegrationEventListener}; this handler
     * publishes to {@code <topic>.DLT} then commits the original offset.
     * In-listener retries already ran — Spring back-off is disabled (0 attempts).
     */
    @Bean(name = "integrationKafkaListenerContainerFactory")
    @ConditionalOnMissingBean(name = "integrationKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> integrationKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 0L));
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
