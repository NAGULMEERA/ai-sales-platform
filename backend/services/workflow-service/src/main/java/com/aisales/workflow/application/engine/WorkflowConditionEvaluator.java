package com.aisales.workflow.application.engine;

import com.aisales.common.contracts.workflow.WorkflowConditionDto;
import com.aisales.common.contracts.workflow.WorkflowConditionType;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Evaluates declarative conditions against trigger context. No scripting language.
 */
@Component
public class WorkflowConditionEvaluator {

    public boolean matches(List<WorkflowConditionDto> conditions, Map<String, Object> context) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        for (WorkflowConditionDto condition : conditions) {
            if (!matchesOne(condition, context)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesOne(WorkflowConditionDto condition, Map<String, Object> context) {
        if (condition == null || condition.getType() == null) {
            return false;
        }
        Map<String, Object> params = condition.getParams() == null ? Map.of() : condition.getParams();
        return switch (condition.getType()) {
            case ALWAYS -> true;
            case LEAD_SCORE_GT -> number(context.get("leadScore")) > number(params.get("value"));
            case OPPORTUNITY_STAGE_EQUALS -> equalsIgnoreCase(
                    string(context.get("opportunityStage")), string(params.get("value")));
            case CUSTOMER_EXISTS -> truthy(context.get("customerExists"))
                    || StringUtils.hasText(string(context.get("customerId")));
            case CONVERSATION_IDLE_MINUTES -> number(context.get("idleMinutes"))
                    >= number(params.get("value"));
            case NO_REPLY_MINUTES -> number(context.get("noReplyMinutes"))
                    >= number(params.get("value"));
            case AI_CONFIDENCE_GTE -> number(context.get("aiConfidence"))
                    >= number(params.get("value"));
            case CATALOG_AVAILABLE -> truthy(context.get("catalogAvailable"));
            case BUSINESS_HOURS -> isBusinessHours(params);
            case TENANT_PLAN_EQUALS -> equalsIgnoreCase(
                    string(context.get("tenantPlan")), string(params.get("value")));
        };
    }

    private boolean isBusinessHours(Map<String, Object> params) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime start = LocalTime.parse(string(params.getOrDefault("start", "09:00")));
        LocalTime end = LocalTime.parse(string(params.getOrDefault("end", "18:00")));
        LocalTime current = now.toLocalTime();
        return !current.isBefore(start) && current.isBefore(end);
    }

    private static double number(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0d;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0d;
        }
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left.equalsIgnoreCase(right);
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return false;
        }
        return "true".equalsIgnoreCase(String.valueOf(value))
                || "1".equals(String.valueOf(value));
    }
}
