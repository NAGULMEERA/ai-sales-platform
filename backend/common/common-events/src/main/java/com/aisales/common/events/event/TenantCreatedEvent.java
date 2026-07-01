package com.aisales.common.events.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class TenantCreatedEvent extends BaseEvent {

    private String tenantId;
    private String name;
}
