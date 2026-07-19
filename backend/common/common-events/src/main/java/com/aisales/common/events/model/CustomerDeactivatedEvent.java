package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomerDeactivatedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "CustomerDeactivated";

    private String customerName;
    private String reason;

    public static CustomerDeactivatedEvent of(
            String tenantId,
            String customerId,
            String customerName,
            String reason,
            String correlationId) {
        CustomerDeactivatedEvent event = new CustomerDeactivatedEvent();
        event.init(EVENT_TYPE, tenantId, customerId, correlationId);
        event.customerName = customerName;
        event.reason = reason;
        return event;
    }
}
