package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SemanticCacheHitEvent extends BaseEvent {

    private String promptScope;
    private String model;

    public static SemanticCacheHitEvent of(
            String tenantId, String promptScope, String model, String correlationId) {
        SemanticCacheHitEvent event = new SemanticCacheHitEvent();
        event.init("SemanticCacheHit", tenantId, promptScope, correlationId);
        event.promptScope = promptScope;
        event.model = model;
        return event;
    }
}
