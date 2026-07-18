package com.aisales.common.events.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
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
        event.setLeadName(leadName);
        event.setScore(score);
        event.setStatus(status);
        return event;
    }
}
