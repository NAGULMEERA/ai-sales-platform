package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomerVerifiedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "CustomerVerified";

    private String customerName;
    private String methodType;
    private String contactValue;

    public static CustomerVerifiedEvent of(
            String tenantId,
            String customerId,
            String customerName,
            String methodType,
            String contactValue,
            String correlationId) {
        CustomerVerifiedEvent event = new CustomerVerifiedEvent();
        event.init(EVENT_TYPE, tenantId, customerId, correlationId);
        event.customerName = customerName;
        event.methodType = methodType;
        event.contactValue = contactValue;
        return event;
    }
}
