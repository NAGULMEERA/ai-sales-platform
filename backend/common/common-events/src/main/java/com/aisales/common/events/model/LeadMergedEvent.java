package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadMergedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "LeadMerged";

    private String leadName;
    private String mergedLeadId;
    private String survivorLeadId;

    public static LeadMergedEvent of(
            String tenantId,
            String survivorLeadId,
            String leadName,
            String mergedLeadId,
            String correlationId) {
        LeadMergedEvent event = new LeadMergedEvent();
        event.init(EVENT_TYPE, tenantId, survivorLeadId, correlationId);
        event.leadName = leadName;
        event.mergedLeadId = mergedLeadId;
        event.survivorLeadId = survivorLeadId;
        return event;
    }
}
