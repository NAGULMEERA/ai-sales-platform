package com.aisales.marketplace.application.service;

import com.aisales.common.contracts.plugin.EnablePluginRequest;
import com.aisales.common.contracts.plugin.PluginCatalogDto;
import com.aisales.common.contracts.plugin.PluginInstallationDto;
import com.aisales.common.contracts.plugin.PluginInstallationStatus;
import com.aisales.common.contracts.plugin.PluginTypeDto;
import com.aisales.common.contracts.plugin.UpdatePluginConfigRequest;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.PluginDisabledEvent;
import com.aisales.common.events.model.PluginEnabledEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.marketplace.application.mapper.PluginMapper;
import com.aisales.marketplace.domain.entity.PluginCatalogEntry;
import com.aisales.marketplace.domain.entity.PluginInstallation;
import com.aisales.marketplace.domain.service.SemVer;
import com.aisales.marketplace.infrastructure.configuration.PlatformVersionProperties;
import com.aisales.marketplace.infrastructure.persistence.PluginCatalogRepository;
import com.aisales.marketplace.infrastructure.persistence.PluginInstallationRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Thin plugin registry: discover catalog entries and enable/disable per tenant.
 * Plugins are metadata/config — this service does not execute plugin business logic.
 */
@Service
@RequiredArgsConstructor
public class PluginRegistryService {

    private final PluginCatalogRepository catalogRepository;
    private final PluginInstallationRepository installationRepository;
    private final PluginMapper mapper;
    private final EventPublisher eventPublisher;
    private final PlatformVersionProperties platformVersionProperties;

    @Transactional(readOnly = true)
    public PageResponse<PluginCatalogDto> listCatalog(int page, int size, PluginTypeDto type) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        Page<PluginCatalogEntry> result = type != null
                ? catalogRepository.findByTypeAndAvailableTrueOrderByDisplayNameAsc(type, pageable)
                : catalogRepository.findByAvailableTrueOrderByDisplayNameAsc(pageable);
        return PageResponse.<PluginCatalogDto>builder()
                .content(result.getContent().stream().map(mapper::toDto).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PluginCatalogDto getCatalogEntry(String pluginKey) {
        return mapper.toDto(requireCatalog(pluginKey));
    }

    @Transactional(readOnly = true)
    public List<PluginInstallationDto> listInstallations() {
        return installationRepository.findByTenantIdOrderByUpdatedAtDesc(requireTenantId()).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public PluginInstallationDto enable(String pluginKey, EnablePluginRequest request) {
        UUID tenantId = requireTenantId();
        UUID actor = actorId();
        PluginCatalogEntry catalog = requireCatalog(pluginKey);
        assertPlatformCompatible(catalog);

        Map<String, Object> config = mergeConfig(catalog.getDefaultConfig(),
                request != null ? request.getConfig() : null);

        PluginInstallation installation = installationRepository
                .findByTenantIdAndPluginKey(tenantId, catalog.getPluginKey())
                .orElseGet(() -> PluginInstallation.builder()
                        .tenantId(tenantId)
                        .pluginKey(catalog.getPluginKey())
                        .version(catalog.getVersion())
                        .status(PluginInstallationStatus.DISABLED)
                        .config(new HashMap<>(catalog.getDefaultConfig()))
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .createdBy(actor)
                        .updatedBy(actor)
                        .build());

        installation.setVersion(catalog.getVersion());
        installation.enable(config, actor);
        PluginInstallation saved = installationRepository.saveAndFlush(installation);

        eventPublisher.publish(PluginEnabledEvent.of(
                tenantId.toString(),
                saved.getId().toString(),
                catalog.getPluginKey(),
                catalog.getType().name(),
                catalog.getVersion(),
                correlationId()));
        return mapper.toDto(saved);
    }

    @Transactional
    public PluginInstallationDto disable(String pluginKey) {
        UUID tenantId = requireTenantId();
        PluginCatalogEntry catalog = requireCatalog(pluginKey);
        PluginInstallation installation = installationRepository
                .findByTenantIdAndPluginKey(tenantId, catalog.getPluginKey())
                .orElseThrow(() -> new NotFoundException(
                        "Plugin is not installed for tenant: " + pluginKey));
        installation.disable(actorId());
        PluginInstallation saved = installationRepository.save(installation);
        eventPublisher.publish(PluginDisabledEvent.of(
                tenantId.toString(),
                saved.getId().toString(),
                catalog.getPluginKey(),
                catalog.getType().name(),
                catalog.getVersion(),
                correlationId()));
        return mapper.toDto(saved);
    }

    @Transactional
    public PluginInstallationDto updateConfig(UUID installationId, UpdatePluginConfigRequest request) {
        PluginInstallation installation = installationRepository
                .findByTenantIdAndId(requireTenantId(), installationId)
                .orElseThrow(() -> new NotFoundException("Installation not found: " + installationId));
        installation.updateConfig(request.getConfig(), actorId());
        return mapper.toDto(installationRepository.save(installation));
    }

    private PluginCatalogEntry requireCatalog(String pluginKey) {
        if (!StringUtils.hasText(pluginKey)) {
            throw new ValidationException("pluginKey is required");
        }
        return catalogRepository.findByPluginKeyAndAvailableTrue(pluginKey.trim())
                .orElseThrow(() -> new NotFoundException("Plugin not found: " + pluginKey));
    }

    private void assertPlatformCompatible(PluginCatalogEntry catalog) {
        String platformVersion = platformVersionProperties.getVersion();
        String required = catalog.getMinPlatformVersion();
        if (!StringUtils.hasText(required)) {
            return;
        }
        if (!SemVer.isAtLeast(platformVersion, required)) {
            throw new ValidationException(
                    "Plugin " + catalog.getPluginKey()
                            + " requires platform version >= " + required
                            + " but running platform is " + platformVersion);
        }
    }

    private static Map<String, Object> mergeConfig(Map<String, Object> defaults, Map<String, Object> overrides) {
        Map<String, Object> merged = new HashMap<>();
        if (defaults != null) {
            merged.putAll(defaults);
        }
        if (overrides != null) {
            merged.putAll(overrides);
        }
        return merged;
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }

    private UUID actorId() {
        String raw = TenantContext.getUserId();
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String correlationId() {
        return CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate);
    }
}
