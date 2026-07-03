package com.aisales.common.events.inbox;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DeadLetterService {

    private final DeadLetterRepository deadLetterRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(ConsumerRecord<String, String> record, String consumerName, String eventId,
                              String eventType, int retryCount, Exception exception) {
        Instant now = Instant.now();
        deadLetterRepository.save(DeadLetterMessage.builder()
                .topic(record.topic())
                .partitionId(record.partition())
                .messageOffset(record.offset())
                .eventId(eventId)
                .eventType(eventType)
                .payload(record.value() != null ? record.value() : "")
                .errorMessage(exception.getMessage())
                .errorClass(exception.getClass().getName())
                .retryCount(retryCount)
                .consumerName(consumerName)
                .lastAttemptAt(now)
                .createdAt(now)
                .build());
    }
}
