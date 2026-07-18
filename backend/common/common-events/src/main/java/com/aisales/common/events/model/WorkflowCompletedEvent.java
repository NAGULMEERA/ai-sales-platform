package com.aisales.common.events.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowCompletedEvent extends BaseEvent {

    private String definitionKey;
    private String businessKey;

    public static WorkflowCompletedEvent of(String tenantId, String workflowId, String definitionKey,
                                            String businessKey, String correlationId) {
        WorkflowCompletedEvent event = new WorkflowCompletedEvent();
        event.init("WorkflowCompleted", tenantId, workflowId, correlationId);
        event.setDefinitionKey(definitionKey);
        event.setBusinessKey(businessKey);
        return event;
    }
}
