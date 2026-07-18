package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomerUpdatedEvent extends BaseEvent {

    private String customerName;
    private String status;

    public static CustomerUpdatedEvent of(String tenantId, String customerId, String customerName,
                                          String status, String correlationId) {
        CustomerUpdatedEvent event = new CustomerUpdatedEvent();
        event.init("CustomerUpdated", tenantId, customerId, correlationId);
        event.customerName = customerName;
        event.status = status;
        return event;
    }
}
