package com.aisales.workflow.application.service;

import com.aisales.common.contracts.workflow.CreateWorkflowRuleRequest;
import com.aisales.common.contracts.workflow.WorkflowAutomationExecutionDto;
import com.aisales.common.contracts.workflow.WorkflowExecutionHistoryDto;
import com.aisales.common.contracts.workflow.WorkflowRuleDto;
import com.aisales.common.core.audit.Auditable;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.workflow.domain.entity.WorkflowAutomationExecution;
import com.aisales.workflow.domain.entity.WorkflowRule;
import com.aisales.workflow.infrastructure.persistence.WorkflowAutomationExecutionRepository;
import com.aisales.workflow.infrastructure.persistence.WorkflowExecutionHistoryRepository;
import com.aisales.workflow.infrastructure.persistence.WorkflowRuleRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WorkflowRuleService {

    private final WorkflowRuleRepository ruleRepository;
    private final WorkflowAutomationExecutionRepository executionRepository;
    private final WorkflowExecutionHistoryRepository historyRepository;

    @Transactional
    @Auditable(action = "WORKFLOW_RULE_CREATED", resourceType = "WORKFLOW_RULE")
    public WorkflowRuleDto create(CreateWorkflowRuleRequest request) {
        UUID tenantId = requireTenantId();
        if (ruleRepository.findByTenantIdAndCodeAndDeletedAtIsNull(tenantId, request.getCode()).isPresent()) {
            throw new ValidationException("Workflow rule code already exists: " + request.getCode());
        }
        Instant now = Instant.now();
        UUID actor = parseUuidOrNull(TenantContext.getUserId());
        WorkflowRule rule = WorkflowRule.builder()
                .tenantId(tenantId)
                .organizationId(parseUuidOrNull(TenantContext.getOrganizationId()))
                .code(request.getCode().trim())
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .triggerType(request.getTriggerType())
                .conditions(request.getConditions() == null ? new ArrayList<>() : new ArrayList<>(request.getConditions()))
                .actions(request.getActions() == null ? new ArrayList<>() : new ArrayList<>(request.getActions()))
                .enabled(request.isEnabled())
                .maxRetries(request.getMaxRetries())
                .retryBackoffSeconds(request.getRetryBackoffSeconds())
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        return toDto(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<WorkflowRuleDto> list() {
        return ruleRepository.findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(requireTenantId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowRuleDto get(UUID id) {
        return toDto(requireRule(id));
    }

    @Transactional
    public WorkflowRuleDto disable(UUID id) {
        WorkflowRule rule = requireRule(id);
        rule.setEnabled(false);
        rule.setUpdatedAt(Instant.now());
        rule.setUpdatedBy(parseUuidOrNull(TenantContext.getUserId()));
        return toDto(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public WorkflowAutomationExecutionDto getExecution(UUID executionId) {
        UUID tenantId = requireTenantId();
        WorkflowAutomationExecution execution = executionRepository
                .findByTenantIdAndId(tenantId, executionId)
                .orElseThrow(() -> new NotFoundException("Workflow execution not found: " + executionId));
        List<WorkflowExecutionHistoryDto> history = historyRepository
                .findByTenantIdAndExecutionIdOrderByOccurredAtAsc(tenantId, executionId)
                .stream()
                .map(h -> WorkflowExecutionHistoryDto.builder()
                        .id(h.getId())
                        .executionId(h.getExecutionId())
                        .stepType(h.getStepType())
                        .stepName(h.getStepName())
                        .status(h.getStatus())
                        .detail(h.getDetail())
                        .occurredAt(h.getOccurredAt())
                        .payload(h.getPayload() == null ? new HashMap<>() : new HashMap<>(h.getPayload()))
                        .build())
                .toList();
        return WorkflowAutomationExecutionDto.builder()
                .id(execution.getId())
                .tenantId(execution.getTenantId())
                .ruleId(execution.getRuleId())
                .ruleCode(execution.getRuleCode())
                .triggerType(execution.getTriggerType())
                .businessKey(execution.getBusinessKey())
                .state(execution.getState())
                .retryCount(execution.getRetryCount())
                .lastError(execution.getLastError())
                .correlationId(execution.getCorrelationId())
                .startedAt(execution.getStartedAt())
                .completedAt(execution.getCompletedAt())
                .context(execution.getContext() == null ? new HashMap<>() : new HashMap<>(execution.getContext()))
                .history(history)
                .build();
    }

    private WorkflowRule requireRule(UUID id) {
        return ruleRepository
                .findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), id)
                .orElseThrow(() -> new NotFoundException("Workflow rule not found: " + id));
    }

    private WorkflowRuleDto toDto(WorkflowRule rule) {
        return WorkflowRuleDto.builder()
                .id(rule.getId())
                .tenantId(rule.getTenantId())
                .code(rule.getCode())
                .name(rule.getName())
                .description(rule.getDescription())
                .triggerType(rule.getTriggerType())
                .conditions(rule.getConditions() == null ? List.of() : List.copyOf(rule.getConditions()))
                .actions(rule.getActions() == null ? List.of() : List.copyOf(rule.getActions()))
                .enabled(rule.isEnabled())
                .maxRetries(rule.getMaxRetries())
                .retryBackoffSeconds(rule.getRetryBackoffSeconds())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .version(rule.getVersion())
                .build();
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }

    private static UUID parseUuidOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
