package com.aisales.integration.infrastructure.persistence;

import com.aisales.integration.domain.entity.IntegrationWebhookEvent;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntegrationWebhookEventRepository extends JpaRepository<IntegrationWebhookEvent, String> {

    @Modifying
    @Query(value = """
            INSERT INTO integration_webhook_event (event_id, provider, event_type, processed_at)
            VALUES (:eventId, :provider, :eventType, :processedAt)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIgnoreConflict(
            @Param("eventId") String eventId,
            @Param("provider") String provider,
            @Param("eventType") String eventType,
            @Param("processedAt") Instant processedAt);
}
