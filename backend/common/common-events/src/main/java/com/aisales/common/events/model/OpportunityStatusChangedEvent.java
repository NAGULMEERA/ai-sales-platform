package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OpportunityStatusChangedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "OpportunityStatusChanged";

    private String customerId;
    private String previousStatus;
    private String status;
    private String reason;

    public static OpportunityStatusChangedEvent of(String tenantId, String opportunityId, String customerId,
                                                   String previousStatus, String status, String reason,
                                                   String correlationId) {
        OpportunityStatusChangedEvent event = new OpportunityStatusChangedEvent();
        event.init(EVENT_TYPE, tenantId, opportunityId, correlationId);
        event.customerId = customerId;
        event.previousStatus = previousStatus;
        event.status = status;
        event.reason = reason;
        return event;
    }
}
