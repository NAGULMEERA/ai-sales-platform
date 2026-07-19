package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuoteSentEvent extends BaseEvent {

    public static final String EVENT_TYPE = "QuoteSent";

    private String opportunityId;
    private String status;
    private String totalAmount;

    public static QuoteSentEvent of(String tenantId, String quoteId, String opportunityId,
                                    String status, String totalAmount, String correlationId) {
        QuoteSentEvent event = new QuoteSentEvent();
        event.init(EVENT_TYPE, tenantId, quoteId, correlationId);
        event.opportunityId = opportunityId;
        event.status = status;
        event.totalAmount = totalAmount;
        return event;
    }
}
