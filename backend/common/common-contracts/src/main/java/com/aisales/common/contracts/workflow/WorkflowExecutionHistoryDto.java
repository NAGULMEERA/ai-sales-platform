package com.aisales.common.contracts.workflow;

import java.time.Instant;
import java.util.HashMap;
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
public class WorkflowExecutionHistoryDto {

    private UUID id;
    private UUID executionId;
    private String stepType;
    private String stepName;
    private String status;
    private String detail;
    private Instant occurredAt;

    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();
}
