package com.aisales.common.contracts.workflow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowAutomationExecutionDto {

    private UUID id;
    private UUID tenantId;
    private UUID ruleId;
    private String ruleCode;
    private WorkflowTriggerType triggerType;
    private String businessKey;
    private String state;
    private int retryCount;
    private String lastError;
    private String correlationId;
    private Instant startedAt;
    private Instant completedAt;

    @Builder.Default
    private Map<String, Object> context = new HashMap<>();

    @Builder.Default
    private List<WorkflowExecutionHistoryDto> history = new ArrayList<>();
}
