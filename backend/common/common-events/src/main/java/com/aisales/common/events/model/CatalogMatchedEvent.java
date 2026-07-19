package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CatalogMatchedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "CatalogMatched";

    private String candidateCount;

    public static CatalogMatchedEvent of(
            String tenantId, String aggregateId, String candidateCount, String correlationId) {
        CatalogMatchedEvent event = new CatalogMatchedEvent();
        event.init(EVENT_TYPE, tenantId, aggregateId, correlationId);
        event.candidateCount = candidateCount;
        return event;
    }
}
