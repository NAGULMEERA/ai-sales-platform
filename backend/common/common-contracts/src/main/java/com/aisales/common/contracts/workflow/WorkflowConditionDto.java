package com.aisales.common.contracts.workflow;

import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowConditionDto {

    @NotNull
    private WorkflowConditionType type;

    @Builder.Default
    private Map<String, Object> params = new HashMap<>();
}
