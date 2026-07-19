package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadScoredEvent extends BaseEvent {

    public static final String EVENT_TYPE = "LeadScored";

    private String leadName;
    private Integer score;
    private String scoreType;

    public static LeadScoredEvent of(String tenantId, String leadId, String leadName,
                                     Integer score, String scoreType, String correlationId) {
        LeadScoredEvent event = new LeadScoredEvent();
        event.init(EVENT_TYPE, tenantId, leadId, correlationId);
        event.leadName = leadName;
        event.score = score;
        event.scoreType = scoreType;
        return event;
    }
}
