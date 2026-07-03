package com.aisales.common.events.config;

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

@Configuration
@EnableKafka
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnBean(ConsumerFactory.class)
public class KafkaEventsAutoConfiguration {

    @Bean(name = "integrationKafkaListenerContainerFactory")
    @ConditionalOnMissingBean(name = "integrationKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> integrationKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}
