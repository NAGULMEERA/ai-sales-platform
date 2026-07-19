package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OpportunityAssignedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "OpportunityAssigned";

    private String customerId;
    private String assignedTo;
    private String previousAssignedTo;

    public static OpportunityAssignedEvent of(String tenantId, String opportunityId, String customerId,
                                              String assignedTo, String previousAssignedTo,
                                              String correlationId) {
        OpportunityAssignedEvent event = new OpportunityAssignedEvent();
        event.init(EVENT_TYPE, tenantId, opportunityId, correlationId);
        event.customerId = customerId;
        event.assignedTo = assignedTo;
        event.previousAssignedTo = previousAssignedTo;
        return event;
    }
}
