package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadVisitCancelledEvent extends BaseEvent {

    public static final String EVENT_TYPE = "LeadVisitCancelled";

    private String leadName;
    private String reason;

    public static LeadVisitCancelledEvent of(String tenantId, String leadId, String leadName,
                                             String reason, String correlationId) {
        LeadVisitCancelledEvent event = new LeadVisitCancelledEvent();
        event.init(EVENT_TYPE, tenantId, leadId, correlationId);
        event.leadName = leadName;
        event.reason = reason;
        return event;
    }
}
