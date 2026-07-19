package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KnowledgeRetrievedEvent extends BaseEvent {

    private String knowledgeBaseId;
    private String retriever;
    private String chunkCount;
    private String queryHash;

    public static KnowledgeRetrievedEvent of(
            String tenantId,
            String knowledgeBaseId,
            String retriever,
            String chunkCount,
            String queryHash,
            String correlationId) {
        KnowledgeRetrievedEvent event = new KnowledgeRetrievedEvent();
        event.init("KnowledgeRetrieved", tenantId, knowledgeBaseId, correlationId);
        event.knowledgeBaseId = knowledgeBaseId;
        event.retriever = retriever;
        event.chunkCount = chunkCount;
        event.queryHash = queryHash;
        return event;
    }
}
