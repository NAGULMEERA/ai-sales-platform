package com.aisales.common.events.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TenantActivatedEvent extends BaseEvent {

    private String tenantName;
    private String slug;

    public static TenantActivatedEvent of(String tenantId, String tenantName, String slug, String correlationId) {
        TenantActivatedEvent event = new TenantActivatedEvent();
        event.init("TenantActivated", tenantId, tenantId, correlationId);
        event.setTenantName(tenantName);
        event.setSlug(slug);
        return event;
    }
}
