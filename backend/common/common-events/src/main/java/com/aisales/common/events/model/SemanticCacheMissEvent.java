package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SemanticCacheMissEvent extends BaseEvent {

    private String promptScope;
    private String model;

    public static SemanticCacheMissEvent of(
            String tenantId, String promptScope, String model, String correlationId) {
        SemanticCacheMissEvent event = new SemanticCacheMissEvent();
        event.init("SemanticCacheMiss", tenantId, promptScope, correlationId);
        event.promptScope = promptScope;
        event.model = model;
        return event;
    }
}
