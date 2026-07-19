package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OpportunityWonEvent extends BaseEvent {

    public static final String EVENT_TYPE = "OpportunityWon";

    private String customerId;
    private String leadId;
    private String reason;

    public static OpportunityWonEvent of(
            String tenantId,
            String opportunityId,
            String customerId,
            String leadId,
            String reason,
            String correlationId) {
        OpportunityWonEvent event = new OpportunityWonEvent();
        event.init(EVENT_TYPE, tenantId, opportunityId, correlationId);
        event.customerId = customerId;
        event.leadId = leadId;
        event.reason = reason;
        return event;
    }
}
