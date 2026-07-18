package com.aisales.common.events.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Short transactional claim of pending outbox rows (SKIP LOCKED).
 * Kafka send happens outside this transaction.
 * Stale DISPATCHING rows (crash mid-send) are reclaimed after the lease expires.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.events.outbox.enabled", havingValue = "true")
public class OutboxClaimService {

    private final OutboxRepository outboxRepository;

    @Value("${aisales.events.outbox.claim-lease-ms:60000}")
    private long claimLeaseMs;

    @Transactional
    public List<OutboxEvent> claimBatch(int batchSize) {
        Instant now = Instant.now();
        Instant staleBefore = now.minus(Duration.ofMillis(Math.max(1_000L, claimLeaseMs)));
        List<OutboxEvent> claimed = outboxRepository.claimPendingEvents(Math.max(1, batchSize), staleBefore);
        for (OutboxEvent event : claimed) {
            event.setStatus(OutboxEvent.OutboxStatus.DISPATCHING);
            event.setClaimedAt(now);
        }
        return outboxRepository.saveAll(claimed);
    }

    @Transactional
    public void save(OutboxEvent event) {
        outboxRepository.save(event);
    }
}
