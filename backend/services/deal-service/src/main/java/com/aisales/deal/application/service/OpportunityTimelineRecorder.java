package com.aisales.deal.application.service;

import com.aisales.deal.domain.entity.OpportunityTimelineEntry;
import com.aisales.deal.infrastructure.persistence.OpportunityTimelineRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpportunityTimelineRecorder {

    private final OpportunityTimelineRepository timelineRepository;

    public void record(
            UUID tenantId, UUID opportunityId, String eventType, String summary, UUID actor) {
        record(tenantId, opportunityId, eventType, summary, Map.of(), actor);
    }

    public void record(
            UUID tenantId,
            UUID opportunityId,
            String eventType,
            String summary,
            Map<String, Object> details,
            UUID actor) {
        timelineRepository.save(OpportunityTimelineEntry.builder()
                .tenantId(tenantId)
                .opportunityId(opportunityId)
                .eventType(eventType)
                .summary(summary)
                .details(details != null ? new HashMap<>(details) : new HashMap<>())
                .createdAt(Instant.now())
                .createdBy(actor)
                .build());
    }
}
