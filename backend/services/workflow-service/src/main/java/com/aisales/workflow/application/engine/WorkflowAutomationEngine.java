package com.aisales.workflow.application.engine;

import com.aisales.common.contracts.workflow.WorkflowActionDto;
import com.aisales.common.contracts.workflow.WorkflowTriggerType;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.events.model.WorkflowCompletedEvent;
import com.aisales.common.events.model.WorkflowTriggeredEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.aisales.workflow.domain.entity.WorkflowAutomationExecution;
import com.aisales.workflow.domain.entity.WorkflowExecutionHistory;
import com.aisales.workflow.domain.entity.WorkflowRule;
import com.aisales.workflow.domain.enums.WorkflowDefinitionKey;
import com.aisales.workflow.infrastructure.persistence.WorkflowAutomationExecutionRepository;
import com.aisales.workflow.infrastructure.persistence.WorkflowExecutionHistoryRepository;
import com.aisales.workflow.infrastructure.persistence.WorkflowRuleRepository;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowAutomationEngine {

    private final WorkflowRuleRepository ruleRepository;
    private final WorkflowAutomationExecutionRepository executionRepository;
    private final WorkflowExecutionHistoryRepository historyRepository;
    private final WorkflowConditionEvaluator conditionEvaluator;
    private final WorkflowActionExecutor actionExecutor;
    private final EventPublisher eventPublisher;
    private final ObjectProvider<PlatformMetrics> platformMetrics;
    private final TransactionTemplate transactionTemplate;

    public void onTrigger(
            String tenantId,
            WorkflowTriggerType triggerType,
            String businessKey,
            Map<String, Object> context,
            String correlationId) {
        UUID tenantUuid = UUID.fromString(tenantId);
        List<WorkflowRule> rules =
                ruleRepository.findByTenantIdAndTriggerTypeAndEnabledTrueAndDeletedAtIsNull(
                        tenantUuid, triggerType);
        Map<String, Object> safeContext = context == null ? Map.of() : context;
        for (WorkflowRule rule : rules) {
            executeRule(rule, businessKey, safeContext, correlationId);
        }
    }

    /**
     * Persists execution start/complete in short transactions; Feign/AI actions run outside TX.
     */
    public void executeRule(
            WorkflowRule rule,
            String businessKey,
            Map<String, Object> context,
            String correlationId) {
        String corr = correlationId != null
                ? correlationId
                : CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate);

        Map<String, Object> enriched = new HashMap<>(context);
        enriched.put("ruleCode", rule.getCode());
        enriched.put("triggerType", rule.getTriggerType().name());

        WorkflowAutomationExecution execution = transactionTemplate.execute(status ->
                startExecution(rule, businessKey, enriched, corr));
        if (execution == null) {
            return;
        }

        PlatformMetrics metrics = platformMetrics.getIfAvailable();
        Timer.Sample sample = metrics == null ? null : metrics.startTimer();

        try {
            if (!conditionEvaluator.matches(rule.getConditions(), enriched)) {
                transactionTemplate.executeWithoutResult(status ->
                        markSkipped(execution.getId(), rule.getTenantId()));
                completeMetrics(metrics, sample, rule.getTenantId(), true);
                return;
            }
            transactionTemplate.executeWithoutResult(status ->
                    appendHistoryById(execution.getId(), "CONDITION", "ALL", "PASSED", null, Map.of()));

            for (WorkflowActionDto action : rule.getActions()) {
                Map<String, Object> result =
                        actionExecutor.execute(action, rule.getTenantId().toString(), businessKey, enriched);
                enriched.putAll(result);
                transactionTemplate.executeWithoutResult(status -> appendHistoryById(
                        execution.getId(),
                        "ACTION",
                        action.getType().name(),
                        "COMPLETED",
                        null,
                        result));
            }

            Map<String, Object> finalContext = new HashMap<>(enriched);
            transactionTemplate.executeWithoutResult(status ->
                    completeExecution(execution.getId(), rule, businessKey, corr, finalContext));
            completeMetrics(metrics, sample, rule.getTenantId(), true);
        } catch (Exception ex) {
            log.warn(
                    "Workflow automation failed rule={} businessKey={}: {}",
                    rule.getCode(),
                    businessKey,
                    ex.getMessage());
            transactionTemplate.executeWithoutResult(status ->
                    scheduleRetryOrFailById(execution.getId(), rule.getId(), rule.getTenantId(),
                            rule.getMaxRetries(), rule.getRetryBackoffSeconds(), ex.getMessage()));
            completeMetrics(metrics, sample, rule.getTenantId(), false);
        }
    }

    public void retryDue(UUID tenantId) {
        List<WorkflowAutomationExecution> due =
                executionRepository.findByTenantIdAndStateAndNextRetryAtLessThanEqual(
                        tenantId, "RETRYING", Instant.now());
        for (WorkflowAutomationExecution execution : due) {
            ruleRepository
                    .findByTenantIdAndIdAndDeletedAtIsNull(tenantId, execution.getRuleId())
                    .ifPresent(rule -> retryExecution(execution, rule));
        }
    }

    private void retryExecution(WorkflowAutomationExecution execution, WorkflowRule rule) {
        transactionTemplate.executeWithoutResult(status -> {
            WorkflowAutomationExecution current = executionRepository.findById(execution.getId()).orElse(null);
            if (current == null) {
                return;
            }
            current.setState("RUNNING");
            current.setNextRetryAt(null);
            current.setUpdatedAt(Instant.now());
            executionRepository.save(current);
        });

        try {
            Map<String, Object> context = new HashMap<>(execution.getContext());
            for (WorkflowActionDto action : rule.getActions()) {
                Map<String, Object> result = actionExecutor.execute(
                        action,
                        rule.getTenantId().toString(),
                        execution.getBusinessKey(),
                        context);
                context.putAll(result);
                transactionTemplate.executeWithoutResult(status -> appendHistoryById(
                        execution.getId(),
                        "ACTION",
                        action.getType().name(),
                        "COMPLETED",
                        "retry",
                        result));
            }
            Map<String, Object> finalContext = new HashMap<>(context);
            transactionTemplate.executeWithoutResult(status -> {
                WorkflowAutomationExecution current =
                        executionRepository.findById(execution.getId()).orElse(null);
                if (current == null) {
                    return;
                }
                current.setContext(finalContext);
                current.setState("COMPLETED");
                current.setCompletedAt(Instant.now());
                current.setUpdatedAt(Instant.now());
                executionRepository.save(current);
                eventPublisher.publish(WorkflowCompletedEvent.of(
                        rule.getTenantId().toString(),
                        current.getId().toString(),
                        WorkflowDefinitionKey.AUTOMATION_RULE_V1.name(),
                        current.getBusinessKey(),
                        current.getCorrelationId()));
            });
        } catch (Exception ex) {
            transactionTemplate.executeWithoutResult(status ->
                    scheduleRetryOrFailById(execution.getId(), rule.getId(), rule.getTenantId(),
                            rule.getMaxRetries(), rule.getRetryBackoffSeconds(), ex.getMessage()));
        }
    }

    private WorkflowAutomationExecution startExecution(
            WorkflowRule rule, String businessKey, Map<String, Object> enriched, String corr) {
        Instant now = Instant.now();
        WorkflowAutomationExecution execution = executionRepository.save(WorkflowAutomationExecution.builder()
                .tenantId(rule.getTenantId())
                .organizationId(rule.getOrganizationId())
                .ruleId(rule.getId())
                .ruleCode(rule.getCode())
                .triggerType(rule.getTriggerType())
                .businessKey(businessKey)
                .state("RUNNING")
                .correlationId(corr)
                .context(enriched)
                .startedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());

        eventPublisher.publish(WorkflowTriggeredEvent.of(
                rule.getTenantId().toString(),
                execution.getId().toString(),
                WorkflowDefinitionKey.AUTOMATION_RULE_V1.name(),
                rule.getCode(),
                rule.getTriggerType().name(),
                businessKey,
                corr));
        appendHistory(execution, "TRIGGER", rule.getTriggerType().name(), "MATCHED", null, enriched);
        return execution;
    }

    private void markSkipped(UUID executionId, UUID tenantId) {
        WorkflowAutomationExecution execution = executionRepository.findById(executionId).orElse(null);
        if (execution == null) {
            return;
        }
        execution.setState("COMPLETED");
        execution.setCompletedAt(Instant.now());
        execution.setUpdatedAt(Instant.now());
        executionRepository.save(execution);
        appendHistory(execution, "CONDITION", "ALL", "SKIPPED", "Conditions not met", Map.of());
    }

    private void completeExecution(
            UUID executionId,
            WorkflowRule rule,
            String businessKey,
            String corr,
            Map<String, Object> finalContext) {
        WorkflowAutomationExecution execution = executionRepository.findById(executionId).orElse(null);
        if (execution == null) {
            return;
        }
        execution.setContext(finalContext);
        execution.setState("COMPLETED");
        execution.setCompletedAt(Instant.now());
        execution.setUpdatedAt(Instant.now());
        executionRepository.save(execution);
        eventPublisher.publish(WorkflowCompletedEvent.of(
                rule.getTenantId().toString(),
                execution.getId().toString(),
                WorkflowDefinitionKey.AUTOMATION_RULE_V1.name(),
                businessKey,
                corr));
    }

    private void scheduleRetryOrFailById(
            UUID executionId,
            UUID ruleId,
            UUID tenantId,
            int maxRetries,
            int retryBackoffSeconds,
            String error) {
        WorkflowAutomationExecution execution = executionRepository.findById(executionId).orElse(null);
        if (execution == null) {
            return;
        }
        execution.setLastError(error);
        execution.setRetryCount(execution.getRetryCount() + 1);
        execution.setUpdatedAt(Instant.now());
        appendHistory(execution, "ERROR", "EXECUTION", "FAILED", error, Map.of());
        if (execution.getRetryCount() <= maxRetries) {
            execution.setState("RETRYING");
            execution.setNextRetryAt(
                    Instant.now().plusSeconds((long) retryBackoffSeconds * execution.getRetryCount()));
        } else {
            execution.setState("FAILED");
            execution.setCompletedAt(Instant.now());
            PlatformMetrics metrics = platformMetrics.getIfAvailable();
            if (metrics != null) {
                metrics.incrementForTenant(MetricNames.WORKFLOW_FAILED, tenantId.toString());
            }
        }
        executionRepository.save(execution);
    }

    private void appendHistoryById(
            UUID executionId,
            String stepType,
            String stepName,
            String status,
            String detail,
            Map<String, Object> payload) {
        WorkflowAutomationExecution execution = executionRepository.findById(executionId).orElse(null);
        if (execution == null) {
            return;
        }
        appendHistory(execution, stepType, stepName, status, detail, payload);
    }

    private void appendHistory(
            WorkflowAutomationExecution execution,
            String stepType,
            String stepName,
            String status,
            String detail,
            Map<String, Object> payload) {
        historyRepository.save(WorkflowExecutionHistory.builder()
                .tenantId(execution.getTenantId())
                .executionId(execution.getId())
                .stepType(stepType)
                .stepName(stepName)
                .status(status)
                .detail(detail)
                .payload(payload == null ? new HashMap<>() : new HashMap<>(payload))
                .occurredAt(Instant.now())
                .build());
    }

    private void completeMetrics(
            PlatformMetrics metrics, Timer.Sample sample, UUID tenantId, boolean success) {
        if (metrics == null) {
            return;
        }
        metrics.incrementForTenant(
                success ? MetricNames.WORKFLOW_EXECUTED : MetricNames.WORKFLOW_FAILED,
                tenantId.toString());
        if (sample != null) {
            metrics.recordTimer(sample, MetricNames.WORKFLOW_DURATION, "success", String.valueOf(success));
        }
    }
}
