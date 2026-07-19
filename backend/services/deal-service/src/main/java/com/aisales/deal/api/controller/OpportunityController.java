package com.aisales.deal.api.controller;

import com.aisales.common.contracts.deal.AddOpportunityNoteRequest;
import com.aisales.common.contracts.deal.AssignOpportunityRequest;
import com.aisales.common.contracts.deal.CloseOpportunityRequest;
import com.aisales.common.contracts.deal.CreateOpportunityRequest;
import com.aisales.common.contracts.deal.OpportunityDto;
import com.aisales.common.contracts.deal.OpportunityStatus;
import com.aisales.common.contracts.deal.ReopenOpportunityRequest;
import com.aisales.common.contracts.deal.ScoreOpportunityRequest;
import com.aisales.common.contracts.deal.UpdateOpportunityRequest;
import com.aisales.common.contracts.deal.UpdateOpportunityStageRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.security.annotation.PreAuthorizeTenant;
import com.aisales.deal.application.service.OpportunityService;
import com.aisales.deal.domain.entity.OpportunityTimelineEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
@PreAuthorizeTenant
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

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Opportunity timeline")
    public ApiResponse<List<OpportunityTimelineEntry>> timeline(@PathVariable UUID id) {
        return ApiResponse.ok(opportunityService.timeline(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an opportunity")
    public ApiResponse<OpportunityDto> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateOpportunityRequest request) {
        return ApiResponse.ok(opportunityService.update(id, request));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign opportunity owner")
    public ApiResponse<OpportunityDto> assign(
            @PathVariable UUID id, @Valid @RequestBody AssignOpportunityRequest request) {
        return ApiResponse.ok(opportunityService.assign(id, request));
    }

    @PostMapping("/{id}/stage")
    @Operation(summary = "Update opportunity stage")
    public ApiResponse<OpportunityDto> updateStage(
            @PathVariable UUID id, @Valid @RequestBody UpdateOpportunityStageRequest request) {
        return ApiResponse.ok(opportunityService.updateStage(id, request));
    }

    @PostMapping("/{id}/notes")
    @Operation(summary = "Add opportunity note")
    public ApiResponse<OpportunityDto> addNote(
            @PathVariable UUID id, @Valid @RequestBody AddOpportunityNoteRequest request) {
        return ApiResponse.ok(opportunityService.addNote(id, request));
    }

    @PostMapping("/{id}/score")
    @Operation(summary = "Score opportunity")
    public ApiResponse<OpportunityDto> score(
            @PathVariable UUID id, @Valid @RequestBody ScoreOpportunityRequest request) {
        return ApiResponse.ok(opportunityService.score(id, request));
    }

    @PostMapping("/{id}/close-won")
    @Operation(summary = "Close opportunity as won")
    public ApiResponse<OpportunityDto> closeWon(
            @PathVariable UUID id, @Valid @RequestBody(required = false) CloseOpportunityRequest request) {
        return ApiResponse.ok(opportunityService.closeWon(
                id, request != null ? request : CloseOpportunityRequest.builder().build()));
    }

    @PostMapping("/{id}/close-lost")
    @Operation(summary = "Close opportunity as lost")
    public ApiResponse<OpportunityDto> closeLost(
            @PathVariable UUID id, @Valid @RequestBody(required = false) CloseOpportunityRequest request) {
        return ApiResponse.ok(opportunityService.closeLost(
                id, request != null ? request : CloseOpportunityRequest.builder().build()));
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reopen a terminal opportunity")
    public ApiResponse<OpportunityDto> reopen(
            @PathVariable UUID id, @Valid @RequestBody(required = false) ReopenOpportunityRequest request) {
        return ApiResponse.ok(opportunityService.reopen(
                id, request != null ? request : ReopenOpportunityRequest.builder().build()));
    }

    @PostMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archive (soft-delete) opportunity")
    public ApiResponse<Void> archive(@PathVariable UUID id) {
        opportunityService.archive(id);
        return ApiResponse.ok(null);
    }
}
