package com.aisales.deal.api.controller;

import com.aisales.common.contracts.deal.AssignOpportunityRequest;
import com.aisales.common.contracts.deal.CreateOpportunityRequest;
import com.aisales.common.contracts.deal.OpportunityDto;
import com.aisales.common.contracts.deal.OpportunityStatus;
import com.aisales.common.contracts.deal.UpdateOpportunityRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.deal.application.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/opportunities")
@RequiredArgsConstructor
@Tag(name = "Opportunities", description = "Sales opportunity aggregate (deal-service)")
public class OpportunityController {

    private final OpportunityService opportunityService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an opportunity")
    public ApiResponse<OpportunityDto> create(@Valid @RequestBody CreateOpportunityRequest request) {
        return ApiResponse.ok(opportunityService.create(request));
    }

    @GetMapping
    @Operation(summary = "Search opportunities")
    public ApiResponse<PageResponse<OpportunityDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OpportunityStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID leadId) {
        return ApiResponse.ok(opportunityService.list(page, size, status, customerId, leadId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an opportunity")
    public ApiResponse<OpportunityDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(opportunityService.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an opportunity")
    public ApiResponse<OpportunityDto> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateOpportunityRequest request) {
        return ApiResponse.ok(opportunityService.update(id, request));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign opportunity owner (manual; does not use lead assignment pool)")
    public ApiResponse<OpportunityDto> assign(
            @PathVariable UUID id, @Valid @RequestBody AssignOpportunityRequest request) {
        return ApiResponse.ok(opportunityService.assign(id, request));
    }
}
