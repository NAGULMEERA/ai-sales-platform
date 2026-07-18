package com.aisales.catalog.api.controller;

import com.aisales.catalog.application.service.CatalogMatchingService;
import com.aisales.common.contracts.catalog.CatalogMatchRequest;
import com.aisales.common.contracts.catalog.CatalogMatchResultDto;
import com.aisales.common.core.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
@Tag(name = "Catalog Matching", description = "Deterministic product/offer matching foundation")
public class CatalogMatchController {

    private final CatalogMatchingService matchingService;

    @PostMapping
    @Operation(summary = "Match catalog products/offers to criteria (not AI)")
    public ApiResponse<CatalogMatchResultDto> match(@Valid @RequestBody CatalogMatchRequest request) {
        return ApiResponse.ok(matchingService.match(request));
    }
}
