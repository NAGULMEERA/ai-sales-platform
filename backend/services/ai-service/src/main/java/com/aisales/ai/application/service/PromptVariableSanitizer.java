package com.aisales.ai.application.service;

import com.aisales.common.exception.exception.ValidationException;
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
    // Broadened blocklist covering common instruction-override / jailbreak phrasing variants.
    // This remains a defense-in-depth soft/hard filter, not a substitute for treating AI output
    // as untrusted; it is intentionally permissive of ordinary business text (see
    // shouldPreserveLegitimateBusinessText) to avoid rejecting legitimate customer content.
    private static final Pattern INJECTION_MARKERS = Pattern.compile(
            "(?i)(ignore\\s+(all\\s+|any\\s+)?(the\\s+)?(previous|prior|above|earlier)\\s+"
                    + "(instructions?|rules?|prompts?|directives?)"
                    + "|disregard\\s+(all\\s+|any\\s+)?(the\\s+)?(previous|prior|above)\\s+"
                    + "(instructions?|rules?|prompts?)"
                    + "|forget\\s+(all\\s+|any\\s+)?(the\\s+)?(previous|prior|above)?\\s*"
                    + "(instructions?|rules?|context)"
                    + "|system\\s*:\\s*you\\s+are"
                    + "|\\bnew\\s+instructions?\\s*:"
                    + "|\\byou\\s+are\\s+now\\b"
                    + "|\\bact\\s+as\\s+(if\\s+you\\s+(are|were)|a)\\b"
                    + "|\\bpretend\\s+(you\\s+are|to\\s+be)\\b"
                    + "|\\breveal\\s+(your\\s+)?(system\\s+prompt|instructions|hidden\\s+prompt)\\b"
                    + "|\\bshow\\s+(me\\s+)?(your\\s+)?(system\\s+prompt|instructions)\\b"
                    + "|\\bprint\\s+(your\\s+)?(system\\s+prompt|instructions)\\b"
                    + "|\\brepeat\\s+(the\\s+)?(words\\s+)?above\\b"
                    + "|\\bjailbreak\\b"
                    + "|\\bdeveloper\\s+mode\\b"
                    + "|\\bunrestricted\\s+mode\\b"
                    + "|\\bno\\s+(more\\s+)?restrictions\\b"
                    + "|\\bOVERRIDE\\s+SYSTEM\\b"
                    + "|\\bDAN\\s+mode\\b)");

    /**
     * Rejects high-risk instruction-override payloads before sanitization.
     * Soft filtering remains as defense-in-depth for obfuscated variants.
     */
    public void rejectIfInjection(Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (looksLikeInjection(entry.getValue())) {
                throw new ValidationException(
                        "Prompt variable rejected: possible instruction override detected");
            }
        }
    }

    public void rejectIfInjection(String value) {
        if (looksLikeInjection(value)) {
            throw new ValidationException(
                    "Prompt input rejected: possible instruction override detected");
        }
    }

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

    /**
     * Soft-filters retrieved knowledge-chunk text before it is assembled into a prompt.
     * RAG content is indirect untrusted input (authored by whoever uploaded the source
     * document, not the caller), so it is never hard-rejected here — that would let one
     * poisoned document break retrieval for an entire knowledge base. Instead this applies
     * the same control-char stripping and instruction-override neutralization used for
     * explicit prompt variables, as defense-in-depth alongside the untrusted-data delimiters
     * already applied by {@code KnowledgeContextAssembler}. No length truncation is applied
     * here; chunk sizing is already bounded by the chunking pipeline.
     */
    public String sanitizeKnowledgeChunk(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw == null ? "" : raw;
        }
        String value = CONTROL_CHARS.matcher(raw).replaceAll("");
        value = value.replace('\u0000', ' ');
        return INJECTION_MARKERS.matcher(value).replaceAll("[filtered]");
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

    /** True when text contains high-risk instruction-override markers. */
    boolean looksLikeInjection(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return INJECTION_MARKERS.matcher(value.toLowerCase(Locale.ROOT)).find();
    }
}
