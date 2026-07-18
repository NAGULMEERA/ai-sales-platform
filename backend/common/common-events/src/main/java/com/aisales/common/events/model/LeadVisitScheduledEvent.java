package com.aisales.common.events.model;

import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadVisitScheduledEvent extends BaseEvent {

    private String leadName;
    private Instant scheduledAt;
    private String location;

    public static LeadVisitScheduledEvent of(String tenantId, String leadId, String leadName,
                                             Instant scheduledAt, String location, String correlationId) {
        LeadVisitScheduledEvent event = new LeadVisitScheduledEvent();
        event.init("LeadVisitScheduled", tenantId, leadId, correlationId);
        event.leadName = leadName;
        event.scheduledAt = scheduledAt;
        event.location = location;
        return event;
    }
}
