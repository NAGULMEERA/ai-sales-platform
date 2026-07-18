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
     */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status = 'PENDING'
               OR (status = 'DISPATCHING'
                   AND claimed_at IS NOT NULL
                   AND claimed_at < :staleBefore)
            ORDER BY created_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> claimPendingEvents(@Param("batchSize") int batchSize,
                                         @Param("staleBefore") Instant staleBefore);

    long countByStatus(OutboxEvent.OutboxStatus status);

    List<OutboxEvent> findByStatus(OutboxEvent.OutboxStatus status);
}
