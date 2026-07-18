package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuoteAcceptedEvent extends BaseEvent {

    private String opportunityId;
    private String status;
    private String totalAmount;

    public static QuoteAcceptedEvent of(String tenantId, String quoteId, String opportunityId,
                                        String status, String totalAmount, String correlationId) {
        QuoteAcceptedEvent event = new QuoteAcceptedEvent();
        event.init("QuoteAccepted", tenantId, quoteId, correlationId);
        event.opportunityId = opportunityId;
        event.status = status;
        event.totalAmount = totalAmount;
        return event;
    }
}
