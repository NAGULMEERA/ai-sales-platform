package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadQualifiedEvent extends BaseEvent {

    private String leadName;
    private Integer score;
    private String status;

    public static LeadQualifiedEvent of(String tenantId, String leadId, String leadName,
                                        Integer score, String status, String correlationId) {
        LeadQualifiedEvent event = new LeadQualifiedEvent();
        event.init("LeadQualified", tenantId, leadId, correlationId);
        event.leadName = leadName;
        event.score = score;
        event.status = status;
        return event;
    }
}
