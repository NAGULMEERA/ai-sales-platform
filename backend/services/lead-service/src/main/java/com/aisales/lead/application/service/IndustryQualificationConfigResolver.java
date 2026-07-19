package com.aisales.lead.application.service;

import com.aisales.common.contracts.client.MarketplaceServiceClient;
import com.aisales.common.contracts.lead.QualifyLeadWithAiRequest;
import com.aisales.common.contracts.plugin.PluginInstallationDto;
import com.aisales.common.contracts.plugin.PluginInstallationStatus;
import com.aisales.common.contracts.plugin.PluginTypeDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.exception.exception.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves qualification promptCode / variableKeys from the tenant's enabled industry
 * plugin config when the caller omits them. Explicit request fields always win.
 */
@Component
@RequiredArgsConstructor
public class IndustryQualificationConfigResolver {

    private final MarketplaceServiceClient marketplaceServiceClient;

    public Resolved resolve(QualifyLeadWithAiRequest request) {
        String promptCode = StringUtils.hasText(request.getPromptCode())
                ? request.getPromptCode().trim().toUpperCase(Locale.ROOT)
                : null;
        List<String> variableKeys = request.getVariableKeys() != null
                ? new ArrayList<>(request.getVariableKeys())
                : new ArrayList<>();

        if (promptCode != null && !variableKeys.isEmpty()) {
            return new Resolved(promptCode, variableKeys);
        }

        PluginInstallationDto industry = findEnabledIndustryWithQualificationConfig();
        if (industry == null) {
            if (promptCode == null) {
                throw new ValidationException(
                        "promptCode is required when no enabled industry plugin provides qualificationPromptCode");
            }
            return new Resolved(promptCode, variableKeys);
        }

        Map<String, Object> config = industry.getConfig() != null ? industry.getConfig() : Map.of();
        if (promptCode == null) {
            promptCode = stringConfig(config, "qualificationPromptCode");
            if (!StringUtils.hasText(promptCode)) {
                throw new ValidationException(
                        "Enabled industry plugin "
                                + industry.getPluginKey()
                                + " is missing qualificationPromptCode");
            }
            promptCode = promptCode.trim().toUpperCase(Locale.ROOT);
        }
        if (variableKeys.isEmpty()) {
            variableKeys.addAll(stringListConfig(config, "qualificationVariableKeys"));
        }
        return new Resolved(promptCode, variableKeys);
    }

    private PluginInstallationDto findEnabledIndustryWithQualificationConfig() {
        ApiResponse<List<PluginInstallationDto>> response = marketplaceServiceClient.listInstallations();
        List<PluginInstallationDto> installations =
                response != null && response.getData() != null ? response.getData() : List.of();

        List<PluginInstallationDto> candidates = installations.stream()
                .filter(i -> i.getStatus() == PluginInstallationStatus.ENABLED)
                .filter(i -> i.getPluginType() == null || i.getPluginType() == PluginTypeDto.INDUSTRY)
                .filter(i -> i.getConfig() != null
                        && StringUtils.hasText(stringConfig(i.getConfig(), "qualificationPromptCode")))
                .toList();

        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() > 1) {
            // Prefer an explicit INDUSTRY type when multiple installs expose the key.
            return candidates.stream()
                    .filter(i -> i.getPluginType() == PluginTypeDto.INDUSTRY)
                    .findFirst()
                    .orElse(candidates.getFirst());
        }
        return candidates.getFirst();
    }

    private static String stringConfig(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value != null ? Objects.toString(value, null) : null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringListConfig(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof List<?> list) {
            List<String> keys = new ArrayList<>();
            for (Object item : list) {
                if (item != null && StringUtils.hasText(String.valueOf(item))) {
                    keys.add(String.valueOf(item).trim());
                }
            }
            return keys;
        }
        return List.of();
    }

    public record Resolved(String promptCode, List<String> variableKeys) {}
}
