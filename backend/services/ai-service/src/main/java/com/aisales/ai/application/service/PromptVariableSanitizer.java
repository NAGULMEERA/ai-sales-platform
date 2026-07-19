package com.aisales.ai.application.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Sanitizes untrusted prompt variables and retrieval queries to reduce prompt-injection risk.
 * Does not change request schema; values are cleaned before template rendering.
 */
@Component
public class PromptVariableSanitizer {

    static final int MAX_VARIABLE_LENGTH = 8_000;
    static final int MAX_QUERY_LENGTH = 2_000;

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");
    private static final Pattern INJECTION_MARKERS = Pattern.compile(
            "(?i)(ignore\\s+(all\\s+)?(previous|prior|above)\\s+instructions"
                    + "|disregard\\s+(all\\s+)?(previous|prior)\\s+instructions"
                    + "|system\\s*:\\s*you\\s+are"
                    + "|\\bOVERRIDE\\s+SYSTEM\\b"
                    + "|\\bDAN\\s+mode\\b)");

    public Map<String, String> sanitizeVariables(Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return Map.of();
        }
        Map<String, String> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            cleaned.put(entry.getKey(), sanitizeValue(entry.getValue(), MAX_VARIABLE_LENGTH));
        }
        return cleaned;
    }

    public String sanitizeRetrievalQuery(String query) {
        return sanitizeValue(query, MAX_QUERY_LENGTH);
    }

    String sanitizeValue(String raw, int maxLength) {
        if (!StringUtils.hasText(raw)) {
            return raw == null ? "" : raw;
        }
        String value = CONTROL_CHARS.matcher(raw).replaceAll("");
        value = value.replace('\u0000', ' ');
        if (value.length() > maxLength) {
            value = value.substring(0, maxLength);
        }
        // Neutralize common instruction-override phrases without rejecting legitimate business text.
        value = INJECTION_MARKERS.matcher(value).replaceAll("[filtered]");
        return value;
    }

    /** True when text still contains high-risk override markers after soft filtering (defense in depth). */
    boolean looksLikeInjection(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return INJECTION_MARKERS.matcher(value.toLowerCase(Locale.ROOT)).find();
    }
}
