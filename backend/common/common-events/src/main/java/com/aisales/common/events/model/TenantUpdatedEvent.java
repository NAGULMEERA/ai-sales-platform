package com.aisales.common.events.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TenantUpdatedEvent extends BaseEvent {

    private String tenantName;
    private String status;

    public static TenantUpdatedEvent of(String tenantId, String tenantName, String status, String correlationId) {
        TenantUpdatedEvent event = new TenantUpdatedEvent();
        event.init("TenantUpdated", tenantId, tenantId, correlationId);
        event.setTenantName(tenantName);
        event.setStatus(status);
        return event;
    }
}
