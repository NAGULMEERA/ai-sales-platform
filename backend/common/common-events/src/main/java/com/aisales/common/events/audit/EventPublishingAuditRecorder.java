package com.aisales.common.events.audit;

import com.aisales.common.core.audit.AuditRecord;
import com.aisales.common.core.audit.AuditRecorder;
import com.aisales.common.events.publisher.EventPublisher;
import lombok.RequiredArgsConstructor;

/**
 * Publishes {@link AuditRecordedEvent} for downstream audit-service ingestion.
 * Enable with {@code aisales.audit.publish-events=true} when outbox/Kafka is configured.
 */
@RequiredArgsConstructor
public class EventPublishingAuditRecorder implements AuditRecorder {

    private final EventPublisher eventPublisher;
    private final String topic;

    @Override
    public void record(AuditRecord record) {
        if (record.getResourceId() == null) {
            return;
        }
        String tenantId = record.getTenantId() != null ? record.getTenantId().toString() : null;
        AuditRecordedEvent event = AuditRecordedEvent.of(
                tenantId,
                record.getUserId(),
                record.getAction(),
                record.getResourceType(),
                record.getResourceId(),
                record.getCorrelationId(),
                record.getDetailsJson());
        eventPublisher.publish(topic, event);
    }
}
