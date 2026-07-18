package com.aisales.marketplace.api.controller;

import com.aisales.common.contracts.plugin.EnablePluginRequest;
import com.aisales.common.contracts.plugin.PluginCatalogDto;
import com.aisales.common.contracts.plugin.PluginInstallationDto;
import com.aisales.common.contracts.plugin.PluginTypeDto;
import com.aisales.common.contracts.plugin.UpdatePluginConfigRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.marketplace.application.service.PluginRegistryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/marketplace")
@RequiredArgsConstructor
@Tag(name = "Plugin Registry", description = "Discover and enable plugins (metadata/config only)")
public class PluginRegistryController {

    private final PluginRegistryService pluginRegistryService;

    @GetMapping("/plugins")
    @Operation(summary = "List available plugins from the catalog")
    public ApiResponse<PageResponse<PluginCatalogDto>> listPlugins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) PluginTypeDto type) {
        return ApiResponse.ok(pluginRegistryService.listCatalog(page, size, type));
    }

    @GetMapping("/plugins/{pluginKey}")
    @Operation(summary = "Get a plugin catalog entry")
    public ApiResponse<PluginCatalogDto> getPlugin(@PathVariable String pluginKey) {
        return ApiResponse.ok(pluginRegistryService.getCatalogEntry(pluginKey));
    }

    @GetMapping("/installations")
    @Operation(summary = "List plugin installations for the current tenant")
    public ApiResponse<List<PluginInstallationDto>> listInstallations() {
        return ApiResponse.ok(pluginRegistryService.listInstallations());
    }

    @PostMapping("/plugins/{pluginKey}/enable")
    @Operation(summary = "Enable a plugin for the current tenant")
    public ApiResponse<PluginInstallationDto> enable(
            @PathVariable String pluginKey,
            @RequestBody(required = false) EnablePluginRequest request) {
        return ApiResponse.ok(pluginRegistryService.enable(
                pluginKey, request != null ? request : EnablePluginRequest.builder().build()));
    }

    @PostMapping("/plugins/{pluginKey}/disable")
    @Operation(summary = "Disable a plugin for the current tenant")
    public ApiResponse<PluginInstallationDto> disable(@PathVariable String pluginKey) {
        return ApiResponse.ok(pluginRegistryService.disable(pluginKey));
    }

    @PutMapping("/installations/{id}/config")
    @Operation(summary = "Update tenant plugin configuration")
    public ApiResponse<PluginInstallationDto> updateConfig(
            @PathVariable UUID id, @Valid @RequestBody UpdatePluginConfigRequest request) {
        return ApiResponse.ok(pluginRegistryService.updateConfig(id, request));
    }
}
