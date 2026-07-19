package com.aisales.common.events.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class KafkaEventsAutoConfigurationTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldApplyListenerConcurrencyFromProperty() {
        KafkaEventsAutoConfiguration configuration = new KafkaEventsAutoConfiguration();
        ConsumerFactory<String, String> consumerFactory = mock(ConsumerFactory.class);
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                configuration.integrationKafkaListenerContainerFactory(consumerFactory, kafkaTemplate, 4);

        assertThat(ReflectionTestUtils.getField(factory, "concurrency")).isEqualTo(4);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldClampNonPositiveConcurrencyToOne() {
        KafkaEventsAutoConfiguration configuration = new KafkaEventsAutoConfiguration();
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                configuration.integrationKafkaListenerContainerFactory(
                        mock(ConsumerFactory.class), mock(KafkaTemplate.class), 0);

        assertThat(ReflectionTestUtils.getField(factory, "concurrency")).isEqualTo(1);
    }
}
