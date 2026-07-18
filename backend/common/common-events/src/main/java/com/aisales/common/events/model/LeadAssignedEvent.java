package com.aisales.common.events.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadAssignedEvent extends BaseEvent {

    private String leadName;
    private String assignedTo;
    private String assignmentReason;

    public static LeadAssignedEvent of(String tenantId, String leadId, String leadName,
                                       String assignedTo, String assignmentReason, String correlationId) {
        LeadAssignedEvent event = new LeadAssignedEvent();
        event.init("LeadAssigned", tenantId, leadId, correlationId);
        event.setLeadName(leadName);
        event.setAssignedTo(assignedTo);
        event.setAssignmentReason(assignmentReason);
        return event;
    }
}
