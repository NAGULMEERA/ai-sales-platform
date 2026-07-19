package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomerCreatedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "CustomerCreated";

    private String customerName;
    private String email;

    public static CustomerCreatedEvent of(String tenantId, String customerId, String customerName,
                                          String email, String correlationId) {
        CustomerCreatedEvent event = new CustomerCreatedEvent();
        event.init(EVENT_TYPE, tenantId, customerId, correlationId);
        event.customerName = customerName;
        event.email = email;
        return event;
    }
}
