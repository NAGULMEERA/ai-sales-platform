package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadCreatedEvent extends BaseEvent {

    private String leadName;
    private String source;
    private String status;

    public static LeadCreatedEvent of(String tenantId, String leadId, String leadName,
                                      String source, String status, String correlationId) {
        LeadCreatedEvent event = new LeadCreatedEvent();
        event.init("LeadCreated", tenantId, leadId, correlationId);
        event.leadName = leadName;
        event.source = source;
        event.status = status;
        return event;
    }
}
