package com.aisales.common.events.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TenantDeletedEvent extends BaseEvent {

    public static TenantDeletedEvent of(String tenantId, String correlationId) {
        TenantDeletedEvent event = new TenantDeletedEvent();
        event.init("TenantDeleted", tenantId, tenantId, correlationId);
        return event;
    }
}
