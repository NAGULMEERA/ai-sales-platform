package com.aisales.common.events.outbox;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Short transactional claim of pending outbox rows (SKIP LOCKED).
 * Kafka send happens outside this transaction.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.events.outbox.enabled", havingValue = "true")
public class OutboxClaimService {

    private final OutboxRepository outboxRepository;

    @Transactional
    public List<OutboxEvent> claimBatch(int batchSize) {
        List<OutboxEvent> claimed = outboxRepository.claimPendingEvents(batchSize);
        for (OutboxEvent event : claimed) {
            event.setStatus(OutboxEvent.OutboxStatus.DISPATCHING);
        }
        return outboxRepository.saveAll(claimed);
    }

    @Transactional
    public void save(OutboxEvent event) {
        outboxRepository.save(event);
    }
}
