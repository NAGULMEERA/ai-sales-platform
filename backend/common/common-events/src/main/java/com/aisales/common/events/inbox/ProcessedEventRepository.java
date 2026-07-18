package com.aisales.common.events.inbox;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEvent.ProcessedEventId> {

    boolean existsByEventIdAndConsumerName(String eventId, String consumerName);

    /**
     * Atomic claim insert. Returns 1 when this consumer claimed the event, 0 when already claimed.
     * Does not abort the surrounding PostgreSQL transaction on conflict.
     */
    @Modifying
    @Query(value = """
            INSERT INTO processed_events (event_id, consumer_name, processed_at)
            VALUES (:eventId, :consumerName, :processedAt)
            ON CONFLICT (event_id, consumer_name) DO NOTHING
            """, nativeQuery = true)
    int insertIgnoreConflict(@Param("eventId") String eventId,
                             @Param("consumerName") String consumerName,
                             @Param("processedAt") Instant processedAt);
}
