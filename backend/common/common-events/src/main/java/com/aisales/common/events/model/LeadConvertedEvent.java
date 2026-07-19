package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadConvertedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "LeadConverted";

    private String leadName;
    private String customerId;

    public static LeadConvertedEvent of(String tenantId, String leadId, String leadName,
                                        String customerId, String correlationId) {
        LeadConvertedEvent event = new LeadConvertedEvent();
        event.init(EVENT_TYPE, tenantId, leadId, correlationId);
        event.leadName = leadName;
        event.customerId = customerId;
        return event;
    }
}
