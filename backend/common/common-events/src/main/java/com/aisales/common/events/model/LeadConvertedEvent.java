package com.aisales.common.events.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadConvertedEvent extends BaseEvent {

    private String leadName;
    private String customerId;

    public static LeadConvertedEvent of(String tenantId, String leadId, String leadName,
                                        String customerId, String correlationId) {
        LeadConvertedEvent event = new LeadConvertedEvent();
        event.init("LeadConverted", tenantId, leadId, correlationId);
        event.setLeadName(leadName);
        event.setCustomerId(customerId);
        return event;
    }
}
