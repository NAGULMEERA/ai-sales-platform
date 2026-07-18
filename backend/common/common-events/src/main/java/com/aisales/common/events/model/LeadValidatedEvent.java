package com.aisales.common.events.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadValidatedEvent extends BaseEvent {

    private String leadName;

    public static LeadValidatedEvent of(String tenantId, String leadId, String leadName, String correlationId) {
        LeadValidatedEvent event = new LeadValidatedEvent();
        event.init("LeadValidated", tenantId, leadId, correlationId);
        event.setLeadName(leadName);
        return event;
    }
}
