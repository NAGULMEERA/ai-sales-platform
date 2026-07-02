package com.aisales.tenant.api.controller;

import com.aisales.common.contracts.tenant.CreateTenantRequest;
import com.aisales.common.contracts.tenant.TenantDto;
import com.aisales.common.contracts.tenant.UpdateTenantRequest;
import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.tenant.application.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Tenant lifecycle management (EPIC-02)")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create tenant", description = "Platform super admin creates a new tenant.")
    public ApiResponse<TenantDto> createTenant(
            @RequestHeader(value = ApiConstants.IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody CreateTenantRequest request,
            HttpServletResponse httpResponse) {
        if (StringUtils.hasText(idempotencyKey)) {
            httpResponse.setHeader(ApiConstants.IDEMPOTENCY_KEY_HEADER, idempotencyKey.trim());
        }
        return withCorrelation(ApiResponse.ok("Tenant created", tenantService.createTenant(request, idempotencyKey)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tenant by id")
    public ApiResponse<TenantDto> getTenant(@PathVariable UUID id) {
        return withCorrelation(ApiResponse.ok(tenantService.getTenant(id)));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get tenant by slug")
    public ApiResponse<TenantDto> getTenantBySlug(@PathVariable String slug) {
        return withCorrelation(ApiResponse.ok(tenantService.getTenantBySlug(slug)));
    }

    @GetMapping
    @Operation(summary = "List tenants", description = "Super admin lists all active tenants.")
    public ApiResponse<List<TenantDto>> listTenants() {
        return withCorrelation(ApiResponse.ok(tenantService.listTenants()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update tenant", description = "Super admin or tenant admin updates their tenant.")
    public ApiResponse<TenantDto> updateTenant(
            @PathVariable UUID id, @Valid @RequestBody UpdateTenantRequest request) {
        return withCorrelation(ApiResponse.ok("Tenant updated", tenantService.updateTenant(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate tenant")
    public ApiResponse<TenantDto> activateTenant(@PathVariable UUID id) {
        return withCorrelation(ApiResponse.ok("Tenant activated", tenantService.activateTenant(id)));
    }

    @PatchMapping("/{id}/suspend")
    @Operation(summary = "Suspend tenant")
    public ApiResponse<TenantDto> suspendTenant(@PathVariable UUID id) {
        return withCorrelation(ApiResponse.ok("Tenant suspended", tenantService.suspendTenant(id)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft delete tenant")
    public void deleteTenant(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
    }

    private <T> ApiResponse<T> withCorrelation(ApiResponse<T> response) {
        response.setCorrelationId(CorrelationIdUtils.getCorrelationId());
        return response;
    }
}
