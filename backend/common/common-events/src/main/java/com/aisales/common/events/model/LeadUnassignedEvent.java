package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadUnassignedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "LeadUnassigned";

    private String leadName;
    private String previousAssignee;
    private String reason;

    public static LeadUnassignedEvent of(
            String tenantId,
            String leadId,
            String leadName,
            String previousAssignee,
            String reason,
            String correlationId) {
        LeadUnassignedEvent event = new LeadUnassignedEvent();
        event.init(EVENT_TYPE, tenantId, leadId, correlationId);
        event.leadName = leadName;
        event.previousAssignee = previousAssignee;
        event.reason = reason;
        return event;
    }
}
