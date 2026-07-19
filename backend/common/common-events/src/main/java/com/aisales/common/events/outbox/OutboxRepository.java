package com.aisales.common.events.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Claims pending rows and stale DISPATCHING rows (lease expired) with SKIP LOCKED
     * so multiple dispatcher instances can run safely.
     *
     * <p>Per-aggregate ordering: event N+1 is not claimed while any earlier event for the
     * same aggregate is still PENDING, DISPATCHING, or FAILED. PENDING rows with
     * {@code claimed_at} in the future are backoff-gated after a failed send.
     */
    @Query(value = """
            SELECT o.* FROM outbox_events o
            WHERE (
                    (o.status = 'PENDING'
                        AND (o.claimed_at IS NULL OR o.claimed_at <= :now))
                    OR (o.status = 'DISPATCHING'
                        AND o.claimed_at IS NOT NULL
                        AND o.claimed_at < :staleBefore)
                )
              AND NOT EXISTS (
                    SELECT 1 FROM outbox_events earlier
                    WHERE earlier.aggregate_type = o.aggregate_type
                      AND earlier.aggregate_id = o.aggregate_id
                      AND earlier.created_at < o.created_at
                      AND earlier.status IN ('PENDING', 'DISPATCHING', 'FAILED')
                )
            ORDER BY o.created_at ASC
            LIMIT :batchSize
            FOR UPDATE OF o SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> claimPendingEvents(
            @Param("batchSize") int batchSize,
            @Param("staleBefore") Instant staleBefore,
            @Param("now") Instant now);

    long countByStatus(OutboxEvent.OutboxStatus status);

    List<OutboxEvent> findByStatus(OutboxEvent.OutboxStatus status);
}
