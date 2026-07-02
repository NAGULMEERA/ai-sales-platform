package com.aisales.common.events.outbox;

import com.aisales.common.events.kafka.EventKafkaHeaderPropagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.events.outbox.enabled", havingValue = "true")
public class OutboxDispatchScheduler {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventKafkaHeaderPropagator headerPropagator;

    @Value("${aisales.events.outbox.max-retries:5}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${aisales.events.outbox.dispatch-interval-ms:5000}")
    @Transactional
    public void dispatchPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findPendingEvents();
        for (OutboxEvent outboxEvent : pending) {
            dispatchOne(outboxEvent);
        }
    }

    private void dispatchOne(OutboxEvent outboxEvent) {
        try {
            String key = outboxEvent.getAggregateId();
            ProducerRecord<String, String> record = headerPropagator.enrichProducerRecord(
                    outboxEvent.getTopic(), key, outboxEvent.getPayload());
            kafkaTemplate.send(record).get();
            outboxEvent.setStatus(OutboxEvent.OutboxStatus.PUBLISHED);
            outboxEvent.setPublishedAt(Instant.now());
        } catch (Exception ex) {
            outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
            if (outboxEvent.getRetryCount() >= maxRetries) {
                outboxEvent.setStatus(OutboxEvent.OutboxStatus.FAILED);
                log.error("Outbox event {} permanently failed after {} retries",
                        outboxEvent.getId(), outboxEvent.getRetryCount(), ex);
            } else {
                log.warn("Outbox event {} dispatch failed (retry {}): {}",
                        outboxEvent.getId(), outboxEvent.getRetryCount(), ex.getMessage());
            }
        }
    }
}
