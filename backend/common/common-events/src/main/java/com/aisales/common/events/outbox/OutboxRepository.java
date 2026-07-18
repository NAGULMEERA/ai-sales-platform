package com.aisales.common.events.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Claims a batch of pending rows with row locks that skip already-locked rows,
     * so multiple dispatcher instances can run safely.
     */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status = 'PENDING'
            ORDER BY created_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> claimPendingEvents(@Param("batchSize") int batchSize);

    long countByStatus(OutboxEvent.OutboxStatus status);

    List<OutboxEvent> findByStatus(OutboxEvent.OutboxStatus status);
}
