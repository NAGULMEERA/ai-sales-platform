package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuoteCreatedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "QuoteCreated";

    private String opportunityId;
    private String status;
    private String currency;
    private String totalAmount;
    private String quoteVersion;

    public static QuoteCreatedEvent of(String tenantId, String quoteId, String opportunityId,
                                       String status, String currency, String totalAmount,
                                       String quoteVersion, String correlationId) {
        QuoteCreatedEvent event = new QuoteCreatedEvent();
        event.init(EVENT_TYPE, tenantId, quoteId, correlationId);
        event.opportunityId = opportunityId;
        event.status = status;
        event.currency = currency;
        event.totalAmount = totalAmount;
        event.quoteVersion = quoteVersion;
        return event;
    }
}
