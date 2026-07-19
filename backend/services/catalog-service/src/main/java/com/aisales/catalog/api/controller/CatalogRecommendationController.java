package com.aisales.catalog.api.controller;

import com.aisales.catalog.application.service.CatalogRecommendationService;
import com.aisales.common.contracts.catalog.CatalogRecommendationRequest;
import com.aisales.common.contracts.catalog.CatalogRecommendationResultDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.security.annotation.PreAuthorizeTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@PreAuthorizeTenant
@Tag(name = "Catalog Recommendations", description = "Hybrid catalog recommendation engine")
public class CatalogRecommendationController {

    private final CatalogRecommendationService recommendationService;

    @PostMapping
    @Operation(summary = "Recommend catalog products using hybrid ranking")
    public ApiResponse<CatalogRecommendationResultDto> recommend(
            @Valid @RequestBody CatalogRecommendationRequest request) {
        return ApiResponse.ok(recommendationService.recommend(request));
    }
}
