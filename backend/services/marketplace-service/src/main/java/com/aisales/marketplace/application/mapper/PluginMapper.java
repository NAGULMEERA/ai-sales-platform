package com.aisales.marketplace.application.mapper;

import com.aisales.common.contracts.plugin.PluginCatalogDto;
import com.aisales.common.contracts.plugin.PluginInstallationDto;
import com.aisales.marketplace.domain.entity.PluginCatalogEntry;
import com.aisales.marketplace.domain.entity.PluginInstallation;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PluginMapper {

    public PluginCatalogDto toDto(PluginCatalogEntry entry) {
        return PluginCatalogDto.builder()
                .id(entry.getId())
                .pluginKey(entry.getPluginKey())
                .type(entry.getType())
                .version(entry.getVersion())
                .minPlatformVersion(entry.getMinPlatformVersion())
                .displayName(entry.getDisplayName())
                .description(entry.getDescription())
                .capabilities(entry.getCapabilities() != null
                        ? List.copyOf(entry.getCapabilities())
                        : List.of())
                .industryCode(entry.getIndustryCode())
                .configSchemaJson(entry.getConfigSchemaJson())
                .defaultConfig(entry.getDefaultConfig() != null
                        ? new HashMap<>(entry.getDefaultConfig())
                        : new HashMap<>())
                .metadata(entry.getMetadata() != null
                        ? new HashMap<>(entry.getMetadata())
                        : new HashMap<>())
                .available(entry.isAvailable())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    public PluginInstallationDto toDto(PluginInstallation installation) {
        return PluginInstallationDto.builder()
                .id(installation.getId())
                .tenantId(installation.getTenantId())
                .pluginKey(installation.getPluginKey())
                .version(installation.getVersion())
                .status(installation.getStatus())
                .config(installation.getConfig() != null
                        ? new HashMap<>(installation.getConfig())
                        : new HashMap<>())
                .enabledAt(installation.getEnabledAt())
                .disabledAt(installation.getDisabledAt())
                .createdAt(installation.getCreatedAt())
                .updatedAt(installation.getUpdatedAt())
                .build();
    }
}
