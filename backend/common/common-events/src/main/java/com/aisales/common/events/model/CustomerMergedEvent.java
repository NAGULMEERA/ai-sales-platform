package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomerMergedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "CustomerMerged";

    private String customerName;
    private String mergedCustomerId;
    private String survivorCustomerId;

    public static CustomerMergedEvent of(
            String tenantId,
            String survivorCustomerId,
            String customerName,
            String mergedCustomerId,
            String correlationId) {
        CustomerMergedEvent event = new CustomerMergedEvent();
        event.init(EVENT_TYPE, tenantId, survivorCustomerId, correlationId);
        event.customerName = customerName;
        event.mergedCustomerId = mergedCustomerId;
        event.survivorCustomerId = survivorCustomerId;
        return event;
    }
}
