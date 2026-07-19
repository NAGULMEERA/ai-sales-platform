package com.aisales.workflow.application.engine;

import com.aisales.common.contracts.ai.AiExecuteRequest;
import com.aisales.common.contracts.client.AiServiceClient;
import com.aisales.common.contracts.client.DealServiceClient;
import com.aisales.common.contracts.client.LeadServiceClient;
import com.aisales.common.contracts.deal.CreateOpportunityRequest;
import com.aisales.common.contracts.deal.UpdateOpportunityRequest;
import com.aisales.common.contracts.lead.AssignLeadRequest;
import com.aisales.common.contracts.lead.UpdateLeadRequest;
import com.aisales.common.contracts.workflow.WorkflowActionDto;
import com.aisales.common.contracts.workflow.WorkflowActionType;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.events.model.NotificationSentEvent;
import com.aisales.common.events.model.WorkflowTriggeredEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.workflow.domain.enums.WorkflowDefinitionKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Executes declarative actions by coordinating owning services (Feign) or publishing events.
 * Business rules remain in owning aggregates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowActionExecutor {

    private final EventPublisher eventPublisher;
    private final ObjectProvider<LeadServiceClient> leadServiceClient;
    private final ObjectProvider<DealServiceClient> dealServiceClient;
    private final ObjectProvider<AiServiceClient> aiServiceClient;

    public Map<String, Object> execute(
            WorkflowActionDto action,
            String tenantId,
            String businessKey,
            Map<String, Object> context) {
        if (action == null || action.getType() == null) {
            throw new ValidationException("Workflow action type is required");
        }
        Map<String, Object> params = action.getParams() == null ? Map.of() : action.getParams();
        return switch (action.getType()) {
            case ASSIGN_LEAD -> assignLead(params, context);
            case UPDATE_LEAD -> updateLead(params, context);
            case CREATE_OPPORTUNITY -> createOpportunity(params, context);
            case UPDATE_OPPORTUNITY -> updateOpportunity(params, context);
            case CALL_AI_GATEWAY -> callAi(params, context, businessKey);
            case SEND_NOTIFICATION, SEND_EMAIL, SEND_WHATSAPP -> sendNotification(
                    action.getType(), tenantId, businessKey, params, context);
            case PUBLISH_EVENT, CREATE_TASK, SCHEDULE_FOLLOW_UP, ESCALATE -> publishCoordinationEvent(
                    action.getType(), tenantId, businessKey, params, context);
        };
    }

    private Map<String, Object> assignLead(Map<String, Object> params, Map<String, Object> context) {
        UUID leadId = uuid(context.get("leadId"), params.get("leadId"));
        UUID assignee = uuid(params.get("assignedTo"), context.get("assignedTo"));
        if (leadId == null || assignee == null) {
            throw new ValidationException("ASSIGN_LEAD requires leadId and assignedTo");
        }
        LeadServiceClient client = require(leadServiceClient.getIfAvailable(), "LeadServiceClient");
        client.assignLead(leadId, AssignLeadRequest.builder().assignedTo(assignee).build());
        return Map.of("leadId", leadId.toString(), "assignedTo", assignee.toString());
    }

    private Map<String, Object> updateLead(Map<String, Object> params, Map<String, Object> context) {
        UUID leadId = uuid(context.get("leadId"), params.get("leadId"));
        if (leadId == null) {
            throw new ValidationException("UPDATE_LEAD requires leadId");
        }
        LeadServiceClient client = require(leadServiceClient.getIfAvailable(), "LeadServiceClient");
        UpdateLeadRequest.UpdateLeadRequestBuilder builder = UpdateLeadRequest.builder();
        if (params.containsKey("customerName")) {
            builder.customerName(String.valueOf(params.get("customerName")));
        }
        if (params.containsKey("email")) {
            builder.email(String.valueOf(params.get("email")));
        }
        if (params.containsKey("phone")) {
            builder.phone(String.valueOf(params.get("phone")));
        }
        client.updateLead(leadId, builder.build());
        return Map.of("leadId", leadId.toString(), "updated", true);
    }

    private Map<String, Object> createOpportunity(
            Map<String, Object> params, Map<String, Object> context) {
        UUID leadId = uuid(context.get("leadId"), params.get("leadId"));
        UUID customerId = uuid(context.get("customerId"), params.get("customerId"));
        if (customerId == null) {
            throw new ValidationException("CREATE_OPPORTUNITY requires customerId");
        }
        DealServiceClient client = require(dealServiceClient.getIfAvailable(), "DealServiceClient");
        var response = client.createOpportunity(CreateOpportunityRequest.builder()
                .leadId(leadId)
                .customerId(customerId)
                .name(string(params.getOrDefault("name", "Workflow opportunity")))
                .build());
        String opportunityId = response != null && response.getData() != null
                ? response.getData().getId().toString()
                : "";
        return Map.of("opportunityId", opportunityId);
    }

    private Map<String, Object> updateOpportunity(
            Map<String, Object> params, Map<String, Object> context) {
        UUID opportunityId = uuid(context.get("opportunityId"), params.get("opportunityId"));
        if (opportunityId == null) {
            throw new ValidationException("UPDATE_OPPORTUNITY requires opportunityId");
        }
        DealServiceClient client = require(dealServiceClient.getIfAvailable(), "DealServiceClient");
        UpdateOpportunityRequest.UpdateOpportunityRequestBuilder builder = UpdateOpportunityRequest.builder();
        if (params.containsKey("name")) {
            builder.name(string(params.get("name")));
        }
        if (params.containsKey("probability")) {
            builder.probability((int) Double.parseDouble(string(params.get("probability"))));
        }
        client.updateOpportunity(opportunityId, builder.build());
        return Map.of("opportunityId", opportunityId.toString(), "updated", true);
    }

    private Map<String, Object> callAi(
            Map<String, Object> params, Map<String, Object> context, String businessKey) {
        AiServiceClient client = require(aiServiceClient.getIfAvailable(), "AiServiceClient");
        Map<String, String> variables = new HashMap<>();
        context.forEach((k, v) -> {
            if (v != null) {
                variables.put(k, String.valueOf(v));
            }
        });
        params.forEach((k, v) -> {
            if (v != null) {
                variables.put(k, String.valueOf(v));
            }
        });
        String promptCode = string(params.getOrDefault("promptCode", "WORKFLOW_ASSIST_V1"));
        var response = client.execute(AiExecuteRequest.builder()
                .promptCode(promptCode)
                .capability("WORKFLOW")
                .variables(variables)
                .businessReference(businessKey)
                .build());
        String executionId = response != null && response.getData() != null
                ? String.valueOf(response.getData().getExecutionId())
                : "";
        return Map.of("aiExecutionId", executionId);
    }

    private Map<String, Object> sendNotification(
            WorkflowActionType type,
            String tenantId,
            String businessKey,
            Map<String, Object> params,
            Map<String, Object> context) {
        String channel = switch (type) {
            case SEND_EMAIL -> "EMAIL";
            case SEND_WHATSAPP -> "WHATSAPP";
            default -> string(params.getOrDefault("channel", "EMAIL"));
        };
        String recipient = string(params.getOrDefault(
                "recipient", context.getOrDefault("recipient", context.get("email"))));
        String template = string(params.getOrDefault("template", "workflow-followup"));
        String correlationId = CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate);
        eventPublisher.publish(NotificationSentEvent.of(
                tenantId,
                UUID.randomUUID().toString(),
                channel,
                recipient,
                template,
                correlationId));
        return Map.of(
                "channel", channel,
                "recipient", recipient,
                "template", template,
                "businessKey", businessKey);
    }

    private Map<String, Object> publishCoordinationEvent(
            WorkflowActionType type,
            String tenantId,
            String businessKey,
            Map<String, Object> params,
            Map<String, Object> context) {
        String correlationId = CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate);
        eventPublisher.publish(WorkflowTriggeredEvent.of(
                tenantId,
                UUID.randomUUID().toString(),
                WorkflowDefinitionKey.AUTOMATION_RULE_V1.name(),
                string(params.getOrDefault("ruleCode", context.get("ruleCode"))),
                type.name(),
                businessKey,
                correlationId));
        log.info("Published coordination action {} for businessKey={}", type, businessKey);
        return Map.of("action", type.name(), "published", true);
    }

    private static UUID uuid(Object primary, Object secondary) {
        UUID first = parseUuid(primary);
        return first != null ? first : parseUuid(secondary);
    }

    private static UUID parseUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        try {
            return UUID.fromString(String.valueOf(value));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new ValidationException(name + " is not available");
        }
        return value;
    }
}
