package com.aisales.lead.application.service;

import com.aisales.common.exception.exception.ValidationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Maps industry-agnostic lead attributes into AI Gateway string variables.
 * Does not interpret industry semantics — callers supply keys from plugin metadata.
 */
@Component
public class LeadQualificationVariableMapper {

    public Map<String, String> toVariables(
            String leadName, Map<String, Object> attributes, List<String> variableKeys) {
        if (variableKeys == null || variableKeys.isEmpty()) {
            throw new ValidationException("variableKeys are required for AI qualification");
        }
        Map<String, Object> attrs = attributes != null ? attributes : Map.of();
        Map<String, String> variables = new LinkedHashMap<>();
        if (StringUtils.hasText(leadName)) {
            variables.put("leadName", leadName.trim());
        }
        for (String key : variableKeys) {
            if (!StringUtils.hasText(key)) {
                continue;
            }
            Object value = attrs.get(key.trim());
            if (value == null || !StringUtils.hasText(String.valueOf(value))) {
                throw new ValidationException("Missing lead attribute for AI variable: " + key);
            }
            variables.put(key.trim(), String.valueOf(value));
        }
        return variables;
    }
}
