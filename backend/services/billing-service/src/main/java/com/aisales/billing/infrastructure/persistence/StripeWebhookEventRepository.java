package com.aisales.billing.infrastructure.persistence;

import com.aisales.billing.domain.entity.StripeWebhookEvent;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, String> {

    /**
     * Atomic claim. Returns 1 when this delivery claimed the event, 0 when already processed.
     */
    @Modifying
    @Query(value = """
            INSERT INTO stripe_webhook_event (event_id, event_type, processed_at)
            VALUES (:eventId, :eventType, :processedAt)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIgnoreConflict(
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("processedAt") Instant processedAt);
}
