package com.aisales.customer.application.service;

import com.aisales.customer.domain.entity.CustomerTimelineEntry;
import com.aisales.customer.infrastructure.persistence.CustomerTimelineRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerTimelineRecorder {

    private final CustomerTimelineRepository timelineRepository;

    public void record(UUID tenantId, UUID customerId, String eventType, String summary, UUID actor) {
        record(tenantId, customerId, eventType, summary, Map.of(), actor);
    }

    public void record(
            UUID tenantId,
            UUID customerId,
            String eventType,
            String summary,
            Map<String, Object> eventData,
            UUID actor) {
        timelineRepository.save(CustomerTimelineEntry.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .eventType(eventType)
                .summary(summary)
                .eventData(eventData != null ? new HashMap<>(eventData) : new HashMap<>())
                .occurredAt(Instant.now())
                .createdBy(actor)
                .build());
    }
}
