package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadConvertedToCustomerEvent extends BaseEvent {

    public static final String EVENT_TYPE = "LeadConvertedToCustomer";

    private String customerName;
    private String leadId;
    private boolean linkedExisting;

    public static LeadConvertedToCustomerEvent of(
            String tenantId,
            String customerId,
            String customerName,
            String leadId,
            boolean linkedExisting,
            String correlationId) {
        LeadConvertedToCustomerEvent event = new LeadConvertedToCustomerEvent();
        event.init(EVENT_TYPE, tenantId, customerId, correlationId);
        event.customerName = customerName;
        event.leadId = leadId;
        event.linkedExisting = linkedExisting;
        return event;
    }
}
