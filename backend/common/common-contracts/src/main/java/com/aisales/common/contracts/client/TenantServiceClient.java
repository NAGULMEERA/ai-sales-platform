package com.aisales.common.contracts.client;

import com.aisales.common.contracts.tenant.CreateTenantRequest;
import com.aisales.common.contracts.tenant.TenantDto;
import com.aisales.common.contracts.tenant.UpdateTenantRequest;
import com.aisales.common.core.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "tenant-service", path = "/api/v1/tenants")
public interface TenantServiceClient {

    @GetMapping("/{id}")
    ApiResponse<TenantDto> getTenant(@PathVariable UUID id);

    @GetMapping("/slug/{slug}")
    ApiResponse<TenantDto> getTenantBySlug(@PathVariable String slug);

    @PostMapping
    ApiResponse<TenantDto> createTenant(@RequestBody CreateTenantRequest request);

    @PutMapping("/{id}")
    ApiResponse<TenantDto> updateTenant(@PathVariable UUID id, @RequestBody UpdateTenantRequest request);
}
