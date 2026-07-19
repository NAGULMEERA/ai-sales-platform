package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TenantSuspendedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "TenantSuspended";

    private String tenantName;
    private String slug;

    public static TenantSuspendedEvent of(String tenantId, String tenantName, String slug, String correlationId) {
        TenantSuspendedEvent event = new TenantSuspendedEvent();
        event.init(EVENT_TYPE, tenantId, tenantId, correlationId);
        event.tenantName = tenantName;
        event.slug = slug;
        return event;
    }
}
