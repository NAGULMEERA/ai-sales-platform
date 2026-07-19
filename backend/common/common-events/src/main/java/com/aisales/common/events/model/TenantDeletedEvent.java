package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TenantDeletedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "TenantDeleted";

    public static TenantDeletedEvent of(String tenantId, String correlationId) {
        TenantDeletedEvent event = new TenantDeletedEvent();
        event.init(EVENT_TYPE, tenantId, tenantId, correlationId);
        return event;
    }
}
