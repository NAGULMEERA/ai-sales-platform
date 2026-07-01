package com.aisales.common.events.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TenantCreatedEvent extends BaseEvent {

    private String tenantName;
    private String plan;

    public static TenantCreatedEvent of(String tenantId, String tenantName, String plan, String correlationId) {
        TenantCreatedEvent event = new TenantCreatedEvent();
        event.init("TenantCreated", tenantId, tenantId, correlationId);
        event.setTenantName(tenantName);
        event.setPlan(plan);
        return event;
    }
}
