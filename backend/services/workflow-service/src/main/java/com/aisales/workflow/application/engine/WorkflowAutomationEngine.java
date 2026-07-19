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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
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
        for (WorkflowRule rule : rules) {
            executeRule(rule, businessKey, context == null ? Map.of() : context, correlationId);
        }
    }

    @Transactional
    public void executeRule(
            WorkflowRule rule,
            String businessKey,
            Map<String, Object> context,
            String correlationId) {
        Instant now = Instant.now();
        String corr = correlationId != null
                ? correlationId
                : CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate);

        Map<String, Object> enriched = new HashMap<>(context);
        enriched.put("ruleCode", rule.getCode());
        enriched.put("triggerType", rule.getTriggerType().name());

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

        PlatformMetrics metrics = platformMetrics.getIfAvailable();
        Timer.Sample sample = metrics == null ? null : metrics.startTimer();

        try {
            if (!conditionEvaluator.matches(rule.getConditions(), enriched)) {
                execution.setState("COMPLETED");
                execution.setCompletedAt(Instant.now());
                execution.setUpdatedAt(Instant.now());
                executionRepository.save(execution);
                appendHistory(execution, "CONDITION", "ALL", "SKIPPED", "Conditions not met", Map.of());
                completeMetrics(metrics, sample, rule.getTenantId(), true);
                return;
            }
            appendHistory(execution, "CONDITION", "ALL", "PASSED", null, Map.of());

            for (WorkflowActionDto action : rule.getActions()) {
                Map<String, Object> result =
                        actionExecutor.execute(action, rule.getTenantId().toString(), businessKey, enriched);
                enriched.putAll(result);
                appendHistory(
                        execution,
                        "ACTION",
                        action.getType().name(),
                        "COMPLETED",
                        null,
                        result);
            }

            execution.setContext(enriched);
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
            completeMetrics(metrics, sample, rule.getTenantId(), true);
        } catch (Exception ex) {
            log.warn(
                    "Workflow automation failed rule={} businessKey={}: {}",
                    rule.getCode(),
                    businessKey,
                    ex.getMessage());
            scheduleRetryOrFail(execution, rule, ex.getMessage());
            completeMetrics(metrics, sample, rule.getTenantId(), false);
        }
    }

    @Transactional
    public void retryDue(UUID tenantId) {
        List<WorkflowAutomationExecution> due =
                executionRepository.findByTenantIdAndStateAndNextRetryAtLessThanEqual(
                        tenantId, "RETRYING", Instant.now());
        for (WorkflowAutomationExecution execution : due) {
            ruleRepository
                    .findByTenantIdAndIdAndDeletedAtIsNull(tenantId, execution.getRuleId())
                    .ifPresent(rule -> {
                        execution.setState("RUNNING");
                        execution.setNextRetryAt(null);
                        execution.setUpdatedAt(Instant.now());
                        executionRepository.save(execution);
                        try {
                            Map<String, Object> context = new HashMap<>(execution.getContext());
                            for (WorkflowActionDto action : rule.getActions()) {
                                Map<String, Object> result = actionExecutor.execute(
                                        action,
                                        tenantId.toString(),
                                        execution.getBusinessKey(),
                                        context);
                                context.putAll(result);
                                appendHistory(
                                        execution,
                                        "ACTION",
                                        action.getType().name(),
                                        "COMPLETED",
                                        "retry",
                                        result);
                            }
                            execution.setContext(context);
                            execution.setState("COMPLETED");
                            execution.setCompletedAt(Instant.now());
                            execution.setUpdatedAt(Instant.now());
                            executionRepository.save(execution);
                            eventPublisher.publish(WorkflowCompletedEvent.of(
                                    tenantId.toString(),
                                    execution.getId().toString(),
                                    WorkflowDefinitionKey.AUTOMATION_RULE_V1.name(),
                                    execution.getBusinessKey(),
                                    execution.getCorrelationId()));
                        } catch (Exception ex) {
                            scheduleRetryOrFail(execution, rule, ex.getMessage());
                        }
                    });
        }
    }

    private void scheduleRetryOrFail(
            WorkflowAutomationExecution execution, WorkflowRule rule, String error) {
        execution.setLastError(error);
        execution.setRetryCount(execution.getRetryCount() + 1);
        execution.setUpdatedAt(Instant.now());
        appendHistory(execution, "ERROR", "EXECUTION", "FAILED", error, Map.of());
        if (execution.getRetryCount() <= rule.getMaxRetries()) {
            execution.setState("RETRYING");
            execution.setNextRetryAt(
                    Instant.now().plusSeconds((long) rule.getRetryBackoffSeconds()
                            * execution.getRetryCount()));
        } else {
            execution.setState("FAILED");
            execution.setCompletedAt(Instant.now());
            PlatformMetrics metrics = platformMetrics.getIfAvailable();
            if (metrics != null) {
                metrics.incrementForTenant(
                        MetricNames.WORKFLOW_FAILED, execution.getTenantId().toString());
            }
        }
        executionRepository.save(execution);
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
