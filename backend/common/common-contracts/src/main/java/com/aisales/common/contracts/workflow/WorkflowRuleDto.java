package com.aisales.common.contracts.workflow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRuleDto {

    private UUID id;
    private UUID tenantId;
    private String code;
    private String name;
    private String description;
    private WorkflowTriggerType triggerType;

    @Builder.Default
    private List<WorkflowConditionDto> conditions = new ArrayList<>();

    @Builder.Default
    private List<WorkflowActionDto> actions = new ArrayList<>();

    private boolean enabled;
    private int maxRetries;
    private int retryBackoffSeconds;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
