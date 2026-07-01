package com.aisales.common.events.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public abstract class BaseEvent {

    private String eventId;
    private String tenantId;
    private String eventType;
    private long timestamp;
}
