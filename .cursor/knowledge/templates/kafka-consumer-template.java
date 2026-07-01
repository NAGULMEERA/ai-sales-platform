package com.company.platform.template.messaging;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Production-ready Kafka Consumer Template.
 */
@Component
public class LeadEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(LeadEventConsumer.class);

    private final IdempotencyStore idempotencyStore;
    private final LeadApplicationService applicationService;

    public LeadEventConsumer(IdempotencyStore idempotencyStore,
                             LeadApplicationService applicationService) {
        this.idempotencyStore = idempotencyStore;
        this.applicationService = applicationService;
    }

    @KafkaListener(topics = "lead.events", groupId = "lead-service")
    public void consume(ConsumerRecord<String, EventEnvelope<LeadQualifiedEvent>> record,
                        Acknowledgment ack) {

        EventEnvelope<LeadQualifiedEvent> event = record.value();

        if (idempotencyStore.alreadyProcessed(event.eventId())) {
            ack.acknowledge();
            return;
        }

        try {
            applicationService.processQualifiedLead(event.payload());

            idempotencyStore.markProcessed(event.eventId());

            log.info("Processed event={} correlationId={}",
                    event.eventType(),
                    event.correlationId());

            ack.acknowledge();

        } catch (Exception ex) {
            log.error("Processing failed eventId={}", event.eventId(), ex);
            throw ex; // Retry/DLQ handled by container
        }
    }
}

/* -------- Supporting Contracts -------- */

interface IdempotencyStore {
    boolean alreadyProcessed(UUID eventId);
    void markProcessed(UUID eventId);
}

interface LeadApplicationService {
    void processQualifiedLead(LeadQualifiedEvent event);
}

record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        UUID correlationId,
        T payload) {}

record LeadQualifiedEvent(UUID leadId, int score) {}
