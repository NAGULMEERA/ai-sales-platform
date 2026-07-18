package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OpportunityCreatedEvent extends BaseEvent {

    private String customerId;
    private String leadId;
    private String name;
    private String status;
    private String assignedTo;

    public static OpportunityCreatedEvent of(String tenantId, String opportunityId, String customerId,
                                             String leadId, String name, String status, String assignedTo,
                                             String correlationId) {
        OpportunityCreatedEvent event = new OpportunityCreatedEvent();
        event.init("OpportunityCreated", tenantId, opportunityId, correlationId);
        event.customerId = customerId;
        event.leadId = leadId;
        event.name = name;
        event.status = status;
        event.assignedTo = assignedTo;
        return event;
    }
}
