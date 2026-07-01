package com.aisales.common.events.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class LeadScoredEvent extends BaseEvent {

    private String leadId;
    private int score;
}
