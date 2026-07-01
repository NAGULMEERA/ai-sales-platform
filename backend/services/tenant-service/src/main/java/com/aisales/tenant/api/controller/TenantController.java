package com.aisales.tenant.api.controller;

import com.aisales.common.contracts.tenant.CreateTenantRequest;
import com.aisales.common.contracts.tenant.TenantDto;
import com.aisales.common.contracts.tenant.UpdateTenantRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.security.annotation.AllowPublic;
import com.aisales.tenant.application.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @AllowPublic
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TenantDto> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return withCorrelation(ApiResponse.ok("Tenant created", tenantService.createTenant(request)));
    }

    @GetMapping("/{id}")
    public ApiResponse<TenantDto> getTenant(@PathVariable UUID id) {
        return withCorrelation(ApiResponse.ok(tenantService.getTenant(id)));
    }

    @AllowPublic
    @GetMapping("/slug/{slug}")
    public ApiResponse<TenantDto> getTenantBySlug(@PathVariable String slug) {
        return withCorrelation(ApiResponse.ok(tenantService.getTenantBySlug(slug)));
    }

    @GetMapping
    public ApiResponse<List<TenantDto>> listTenants() {
        return withCorrelation(ApiResponse.ok(tenantService.listTenants()));
    }

    @PutMapping("/{id}")
    public ApiResponse<TenantDto> updateTenant(
            @PathVariable UUID id, @Valid @RequestBody UpdateTenantRequest request) {
        return withCorrelation(ApiResponse.ok(tenantService.updateTenant(id, request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTenant(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
    }

    private <T> ApiResponse<T> withCorrelation(ApiResponse<T> response) {
        response.setCorrelationId(CorrelationIdUtils.getCorrelationId());
        return response;
    }
}
