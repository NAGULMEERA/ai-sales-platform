package com.aisales.common.events.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TenantSuspendedEvent extends BaseEvent {

    private String tenantName;
    private String slug;

    public static TenantSuspendedEvent of(String tenantId, String tenantName, String slug, String correlationId) {
        TenantSuspendedEvent event = new TenantSuspendedEvent();
        event.init("TenantSuspended", tenantId, tenantId, correlationId);
        event.setTenantName(tenantName);
        event.setSlug(slug);
        return event;
    }
}
