package com.aisales.common.contracts.workflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowRuleRequest {

    @NotBlank
    @Size(max = 100)
    private String code;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull
    private WorkflowTriggerType triggerType;

    @NotEmpty
    @Valid
    @Builder.Default
    private List<WorkflowConditionDto> conditions = new ArrayList<>();

    @NotEmpty
    @Valid
    @Builder.Default
    private List<WorkflowActionDto> actions = new ArrayList<>();

    @Builder.Default
    private boolean enabled = true;

    @Min(0)
    @Max(10)
    @Builder.Default
    private int maxRetries = 3;

    @Min(1)
    @Max(3600)
    @Builder.Default
    private int retryBackoffSeconds = 30;
}
