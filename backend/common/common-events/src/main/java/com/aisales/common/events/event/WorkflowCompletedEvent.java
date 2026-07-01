package com.aisales.common.events.event;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class WorkflowCompletedEvent extends BaseEvent {

    private String workflowId;
}
