package com.aisales.ai.api.controller;

import com.aisales.ai.application.service.AiQuotaService;
import com.aisales.common.contracts.ai.AiQuotaStatusDto;
import com.aisales.common.contracts.ai.UpsertAiQuotaRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.security.annotation.PreAuthorizeTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai-quota")
@RequiredArgsConstructor
@Tag(name = "AI Quota", description = "Per-tenant daily AI token budgets (AI_003 when exceeded)")
public class AiQuotaController {

    private final AiQuotaService aiQuotaService;

    @GetMapping
    @PreAuthorizeTenant
    @Operation(summary = "Current tenant AI quota status (limits, used, remaining)")
    public ApiResponse<AiQuotaStatusDto> getStatus() {
        return ApiResponse.ok(aiQuotaService.getStatus(requireTenantId()));
    }

    @PutMapping
    @PreAuthorizeTenant
    @Operation(summary = "Upsert tenant AI quota override (admin)")
    public ApiResponse<AiQuotaStatusDto> upsert(@Valid @RequestBody UpsertAiQuotaRequest request) {
        return ApiResponse.ok(aiQuotaService.upsert(requireTenantId(), request));
    }

    @PutMapping("/plan-package")
    @PreAuthorizeTenant
    @Operation(summary = "Apply a configured plan package (FREE / PREMIUM) to this tenant")
    public ApiResponse<AiQuotaStatusDto> applyPlanPackage(@RequestParam String plan) {
        return ApiResponse.ok(aiQuotaService.applyPlanPackage(requireTenantId(), plan));
    }

    private static UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }
}
