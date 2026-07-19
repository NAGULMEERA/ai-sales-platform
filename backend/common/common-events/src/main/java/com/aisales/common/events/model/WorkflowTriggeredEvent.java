package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowTriggeredEvent extends BaseEvent {

    private String definitionKey;
    private String ruleCode;
    private String triggerType;
    private String businessKey;

    public static WorkflowTriggeredEvent of(
            String tenantId,
            String workflowId,
            String definitionKey,
            String ruleCode,
            String triggerType,
            String businessKey,
            String correlationId) {
        WorkflowTriggeredEvent event = new WorkflowTriggeredEvent();
        event.init("WorkflowTriggered", tenantId, workflowId, correlationId);
        event.definitionKey = definitionKey;
        event.ruleCode = ruleCode;
        event.triggerType = triggerType;
        event.businessKey = businessKey;
        return event;
    }
}
