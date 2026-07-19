package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadStatusChangedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "LeadStatusChanged";

    private String leadName;
    private String oldStatus;
    private String newStatus;
    private String reason;

    public static LeadStatusChangedEvent of(String tenantId, String leadId, String leadName,
                                            String oldStatus, String newStatus, String reason,
                                            String correlationId) {
        LeadStatusChangedEvent event = new LeadStatusChangedEvent();
        event.init(EVENT_TYPE, tenantId, leadId, correlationId);
        event.leadName = leadName;
        event.oldStatus = oldStatus;
        event.newStatus = newStatus;
        event.reason = reason;
        return event;
    }
}
