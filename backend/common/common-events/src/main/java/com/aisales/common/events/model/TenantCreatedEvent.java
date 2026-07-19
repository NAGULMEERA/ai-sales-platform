package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TenantCreatedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "TenantCreated";

    private String tenantName;
    private String slug;
    private String plan;
    private String industry;

    public static TenantCreatedEvent of(String tenantId, String tenantName, String slug,
                                        String plan, String industry, String correlationId) {
        TenantCreatedEvent event = new TenantCreatedEvent();
        event.init(EVENT_TYPE, tenantId, tenantId, correlationId);
        event.tenantName = tenantName;
        event.slug = slug;
        event.plan = plan;
        event.industry = industry;
        return event;
    }
}
