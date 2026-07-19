package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadVisitCompletedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "LeadVisitCompleted";

    private String leadName;
    private String notes;

    public static LeadVisitCompletedEvent of(String tenantId, String leadId, String leadName,
                                             String notes, String correlationId) {
        LeadVisitCompletedEvent event = new LeadVisitCompletedEvent();
        event.init(EVENT_TYPE, tenantId, leadId, correlationId);
        event.leadName = leadName;
        event.notes = notes;
        return event;
    }
}
