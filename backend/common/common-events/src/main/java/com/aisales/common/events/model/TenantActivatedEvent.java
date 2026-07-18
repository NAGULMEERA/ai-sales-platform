package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TenantActivatedEvent extends BaseEvent {

    private String tenantName;
    private String slug;

    public static TenantActivatedEvent of(String tenantId, String tenantName, String slug, String correlationId) {
        TenantActivatedEvent event = new TenantActivatedEvent();
        event.init("TenantActivated", tenantId, tenantId, correlationId);
        event.tenantName = tenantName;
        event.slug = slug;
        return event;
    }
}
