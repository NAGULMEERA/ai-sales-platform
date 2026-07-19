package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadArchivedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "LeadArchived";

    private String leadName;
    private String reason;

    public static LeadArchivedEvent of(String tenantId, String leadId, String leadName,
                                       String reason, String correlationId) {
        LeadArchivedEvent event = new LeadArchivedEvent();
        event.init(EVENT_TYPE, tenantId, leadId, correlationId);
        event.leadName = leadName;
        event.reason = reason;
        return event;
    }
}
