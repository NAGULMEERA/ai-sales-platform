package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OpportunityLostEvent extends BaseEvent {

    private String customerId;
    private String leadId;
    private String reason;

    public static OpportunityLostEvent of(
            String tenantId,
            String opportunityId,
            String customerId,
            String leadId,
            String reason,
            String correlationId) {
        OpportunityLostEvent event = new OpportunityLostEvent();
        event.init("OpportunityLost", tenantId, opportunityId, correlationId);
        event.customerId = customerId;
        event.leadId = leadId;
        event.reason = reason;
        return event;
    }
}
