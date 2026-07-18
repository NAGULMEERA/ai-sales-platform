package com.aisales.common.contracts.client;

import com.aisales.common.contracts.plugin.EnablePluginRequest;
import com.aisales.common.contracts.plugin.PluginCatalogDto;
import com.aisales.common.contracts.plugin.PluginInstallationDto;
import com.aisales.common.contracts.plugin.PluginTypeDto;
import com.aisales.common.contracts.plugin.UpdatePluginConfigRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "marketplace-service",
        path = "/api/v1/marketplace",
        url = "${aisales.clients.marketplace-service.url:}")
public interface MarketplaceServiceClient {

    @GetMapping("/plugins")
    ApiResponse<PageResponse<PluginCatalogDto>> listPlugins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) PluginTypeDto type);

    @GetMapping("/plugins/{pluginKey}")
    ApiResponse<PluginCatalogDto> getPlugin(@PathVariable String pluginKey);

    @GetMapping("/installations")
    ApiResponse<List<PluginInstallationDto>> listInstallations();

    @PostMapping("/plugins/{pluginKey}/enable")
    ApiResponse<PluginInstallationDto> enablePlugin(
            @PathVariable String pluginKey, @RequestBody EnablePluginRequest request);

    @PostMapping("/plugins/{pluginKey}/disable")
    ApiResponse<PluginInstallationDto> disablePlugin(@PathVariable String pluginKey);

    @PutMapping("/installations/{id}/config")
    ApiResponse<PluginInstallationDto> updateConfig(
            @PathVariable UUID id, @RequestBody UpdatePluginConfigRequest request);
}
