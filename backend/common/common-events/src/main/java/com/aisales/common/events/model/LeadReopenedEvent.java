package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LeadReopenedEvent extends BaseEvent {

    private String leadName;
    private String fromStatus;
    private String toStatus;
    private String reason;

    public static LeadReopenedEvent of(
            String tenantId,
            String leadId,
            String leadName,
            String fromStatus,
            String toStatus,
            String reason,
            String correlationId) {
        LeadReopenedEvent event = new LeadReopenedEvent();
        event.init("LeadReopened", tenantId, leadId, correlationId);
        event.leadName = leadName;
        event.fromStatus = fromStatus;
        event.toStatus = toStatus;
        event.reason = reason;
        return event;
    }
}
