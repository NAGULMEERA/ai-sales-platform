package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KnowledgeDocumentRegisteredEvent extends BaseEvent {

    public static final String EVENT_TYPE = "KnowledgeDocumentRegistered";

    private String knowledgeBaseId;
    private String name;
    private String status;
    private String mediaId;

    public static KnowledgeDocumentRegisteredEvent of(String tenantId, String documentId,
                                                      String knowledgeBaseId, String name,
                                                      String status, String mediaId,
                                                      String correlationId) {
        KnowledgeDocumentRegisteredEvent event = new KnowledgeDocumentRegisteredEvent();
        event.init(EVENT_TYPE, tenantId, documentId, correlationId);
        event.knowledgeBaseId = knowledgeBaseId;
        event.name = name;
        event.status = status;
        event.mediaId = mediaId;
        return event;
    }
}
