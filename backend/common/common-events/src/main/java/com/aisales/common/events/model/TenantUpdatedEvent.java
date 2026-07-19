package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TenantUpdatedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "TenantUpdated";

    private String tenantName;
    private String status;

    public static TenantUpdatedEvent of(String tenantId, String tenantName, String status, String correlationId) {
        TenantUpdatedEvent event = new TenantUpdatedEvent();
        event.init(EVENT_TYPE, tenantId, tenantId, correlationId);
        event.tenantName = tenantName;
        event.status = status;
        return event;
    }
}
