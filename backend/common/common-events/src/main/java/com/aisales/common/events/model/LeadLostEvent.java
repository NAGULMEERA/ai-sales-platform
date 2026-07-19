package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadLostEvent extends BaseEvent {

    public static final String EVENT_TYPE = "LeadLost";

    private String leadName;
    private String reason;

    public static LeadLostEvent of(String tenantId, String leadId, String leadName,
                                   String reason, String correlationId) {
        LeadLostEvent event = new LeadLostEvent();
        event.init(EVENT_TYPE, tenantId, leadId, correlationId);
        event.leadName = leadName;
        event.reason = reason;
        return event;
    }
}
