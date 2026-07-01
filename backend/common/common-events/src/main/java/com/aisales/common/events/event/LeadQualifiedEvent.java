package com.aisales.common.events.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class LeadQualifiedEvent extends BaseEvent {

    private String leadId;
    private String qualifiedBy;
}
