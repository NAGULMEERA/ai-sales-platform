package com.aisales.ai.api.controller;

import com.aisales.ai.application.service.PromptService;
import com.aisales.common.contracts.ai.CreatePromptRequest;
import com.aisales.common.contracts.ai.CreatePromptVersionRequest;
import com.aisales.common.contracts.ai.PromptDto;
import com.aisales.common.contracts.ai.PromptVersionDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.security.annotation.PreAuthorizeTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
@PreAuthorizeTenant
@PreAuthorize("hasAuthority('prompt:manage') or hasAuthority('tenant:admin') or hasAnyRole('TENANT_ADMIN','ADMIN','SUPER_ADMIN')")
@Tag(name = "Prompts", description = "Versioned prompt registry")
public class PromptController {

    private final PromptService promptService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a prompt template with version 1")
    public ApiResponse<PromptDto> create(@Valid @RequestBody CreatePromptRequest request) {
        return ApiResponse.ok(promptService.create(request));
    }

    @GetMapping
    @Operation(summary = "List prompts")
    public ApiResponse<PageResponse<PromptDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(promptService.list(page, size));
    }

    @GetMapping("/by-code/{code}")
    @Operation(summary = "Get prompt by code")
    public ApiResponse<PromptDto> getByCode(@PathVariable String code) {
        return ApiResponse.ok(promptService.getByCode(code));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get prompt by id")
    public ApiResponse<PromptDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(promptService.get(id));
    }

    @PostMapping("/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new immutable prompt version")
    public ApiResponse<PromptVersionDto> createVersion(
            @PathVariable UUID id, @Valid @RequestBody CreatePromptVersionRequest request) {
        return ApiResponse.ok(promptService.createVersion(id, request));
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "List prompt versions")
    public ApiResponse<List<PromptVersionDto>> listVersions(@PathVariable UUID id) {
        return ApiResponse.ok(promptService.listVersions(id));
    }

    @PostMapping("/{id}/versions/{versionNumber}/approve")
    @Operation(summary = "Approve a draft prompt version")
    public ApiResponse<PromptVersionDto> approveVersion(
            @PathVariable UUID id, @PathVariable int versionNumber) {
        return ApiResponse.ok(promptService.approveVersion(id, versionNumber));
    }

    @PostMapping("/{id}/versions/{versionNumber}/activate")
    @Operation(summary = "Activate a prompt version (archives previous ACTIVE)")
    public ApiResponse<PromptDto> activateVersion(
            @PathVariable UUID id, @PathVariable int versionNumber) {
        return ApiResponse.ok(promptService.activateVersion(id, versionNumber));
    }

    @PostMapping("/{id}/versions/{versionNumber}/rollback")
    @Operation(summary = "Rollback by reactivating a prior prompt version")
    public ApiResponse<PromptDto> rollbackVersion(
            @PathVariable UUID id, @PathVariable int versionNumber) {
        return ApiResponse.ok(promptService.rollbackToVersion(id, versionNumber));
    }
}
