package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KnowledgeBaseCreatedEvent extends BaseEvent {

    private String code;
    private String name;
    private String status;

    public static KnowledgeBaseCreatedEvent of(String tenantId, String knowledgeBaseId, String code,
                                               String name, String status, String correlationId) {
        KnowledgeBaseCreatedEvent event = new KnowledgeBaseCreatedEvent();
        event.init("KnowledgeBaseCreated", tenantId, knowledgeBaseId, correlationId);
        event.code = code;
        event.name = name;
        event.status = status;
        return event;
    }
}
