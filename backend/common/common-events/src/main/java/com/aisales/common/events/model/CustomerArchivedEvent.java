package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomerArchivedEvent extends BaseEvent {

    private String customerName;
    private String reason;

    public static CustomerArchivedEvent of(String tenantId, String customerId, String customerName,
                                           String reason, String correlationId) {
        CustomerArchivedEvent event = new CustomerArchivedEvent();
        event.init("CustomerArchived", tenantId, customerId, correlationId);
        event.customerName = customerName;
        event.reason = reason;
        return event;
    }
}
