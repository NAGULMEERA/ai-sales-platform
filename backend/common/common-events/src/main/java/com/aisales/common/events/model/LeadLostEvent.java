package com.aisales.common.events.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadLostEvent extends BaseEvent {

    private String leadName;
    private String reason;

    public static LeadLostEvent of(String tenantId, String leadId, String leadName,
                                   String reason, String correlationId) {
        LeadLostEvent event = new LeadLostEvent();
        event.init("LeadLost", tenantId, leadId, correlationId);
        event.setLeadName(leadName);
        event.setReason(reason);
        return event;
    }
}
