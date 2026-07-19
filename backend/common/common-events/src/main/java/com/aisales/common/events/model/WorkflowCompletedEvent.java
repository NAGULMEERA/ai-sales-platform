package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowCompletedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "WorkflowCompleted";

    private String definitionKey;
    private String businessKey;

    public static WorkflowCompletedEvent of(String tenantId, String workflowId, String definitionKey,
                                            String businessKey, String correlationId) {
        WorkflowCompletedEvent event = new WorkflowCompletedEvent();
        event.init(EVENT_TYPE, tenantId, workflowId, correlationId);
        event.definitionKey = definitionKey;
        event.businessKey = businessKey;
        return event;
    }
}
