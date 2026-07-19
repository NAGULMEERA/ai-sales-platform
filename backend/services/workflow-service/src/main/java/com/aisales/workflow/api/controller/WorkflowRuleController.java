package com.aisales.workflow.api.controller;

import com.aisales.common.contracts.workflow.CreateWorkflowRuleRequest;
import com.aisales.common.contracts.workflow.WorkflowAutomationExecutionDto;
import com.aisales.common.contracts.workflow.WorkflowRuleDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.security.annotation.PreAuthorizeTenant;
import com.aisales.workflow.application.service.WorkflowRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@PreAuthorizeTenant
@Tag(name = "Workflow Automation", description = "Trigger/condition/action workflow rules")
public class WorkflowRuleController {

    private final WorkflowRuleService workflowRuleService;

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a workflow automation rule")
    public ApiResponse<WorkflowRuleDto> create(@Valid @RequestBody CreateWorkflowRuleRequest request) {
        return ApiResponse.ok(workflowRuleService.create(request));
    }

    @GetMapping("/rules")
    @Operation(summary = "List workflow automation rules")
    public ApiResponse<List<WorkflowRuleDto>> list() {
        return ApiResponse.ok(workflowRuleService.list());
    }

    @GetMapping("/rules/{id}")
    @Operation(summary = "Get a workflow automation rule")
    public ApiResponse<WorkflowRuleDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(workflowRuleService.get(id));
    }

    @PostMapping("/rules/{id}/disable")
    @Operation(summary = "Disable a workflow automation rule")
    public ApiResponse<WorkflowRuleDto> disable(@PathVariable UUID id) {
        return ApiResponse.ok(workflowRuleService.disable(id));
    }

    @GetMapping("/executions/{id}")
    @Operation(summary = "Get workflow automation execution history")
    public ApiResponse<WorkflowAutomationExecutionDto> getExecution(@PathVariable UUID id) {
        return ApiResponse.ok(workflowRuleService.getExecution(id));
    }
}
