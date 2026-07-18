package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadValidatedEvent extends BaseEvent {

    private String leadName;

    public static LeadValidatedEvent of(String tenantId, String leadId, String leadName, String correlationId) {
        LeadValidatedEvent event = new LeadValidatedEvent();
        event.init("LeadValidated", tenantId, leadId, correlationId);
        event.leadName = leadName;
        return event;
    }
}
