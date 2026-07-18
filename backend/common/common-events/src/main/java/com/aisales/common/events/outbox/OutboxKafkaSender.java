package com.aisales.common.events.outbox;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Isolates outbox Kafka publishes behind a bulkhead so a slow broker cannot exhaust
 * dispatcher threads across all pending events.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.events.outbox.enabled", havingValue = "true")
public class OutboxKafkaSender {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Bulkhead(name = "outboxKafka")
    public void send(ProducerRecord<String, String> record)
            throws ExecutionException, InterruptedException {
        kafkaTemplate.send(record).get();
    }
}
