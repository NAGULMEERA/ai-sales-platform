package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadContactedEvent extends BaseEvent {

    private String leadName;
    private String channel;

    public static LeadContactedEvent of(String tenantId, String leadId, String leadName,
                                        String channel, String correlationId) {
        LeadContactedEvent event = new LeadContactedEvent();
        event.init("LeadContacted", tenantId, leadId, correlationId);
        event.leadName = leadName;
        event.channel = channel;
        return event;
    }
}
