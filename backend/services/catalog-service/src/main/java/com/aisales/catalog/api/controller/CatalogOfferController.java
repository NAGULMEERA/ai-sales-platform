package com.aisales.catalog.api.controller;

import com.aisales.catalog.application.service.CatalogService;
import com.aisales.common.contracts.catalog.CatalogItemStatus;
import com.aisales.common.contracts.catalog.CatalogOfferDto;
import com.aisales.common.contracts.catalog.CreateCatalogOfferRequest;
import com.aisales.common.contracts.catalog.UpdateCatalogOfferRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Catalog Offers", description = "Priced offers for catalog products")
public class CatalogOfferController {

    private final CatalogService catalogService;

    @PostMapping("/offers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a priced offer")
    public ApiResponse<CatalogOfferDto> create(@Valid @RequestBody CreateCatalogOfferRequest request) {
        return ApiResponse.ok(catalogService.createOffer(request));
    }

    @GetMapping("/offers")
    @Operation(summary = "List offers")
    public ApiResponse<PageResponse<CatalogOfferDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) CatalogItemStatus status) {
        return ApiResponse.ok(catalogService.listOffers(page, size, productId, status));
    }

    @GetMapping("/offers/{id}")
    @Operation(summary = "Get an offer")
    public ApiResponse<CatalogOfferDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(catalogService.getOffer(id));
    }

    @GetMapping("/products/{productId}/offers")
    @Operation(summary = "List offers for a product")
    public ApiResponse<List<CatalogOfferDto>> listForProduct(@PathVariable UUID productId) {
        return ApiResponse.ok(catalogService.listOffersForProduct(productId));
    }

    @PutMapping("/offers/{id}")
    @Operation(summary = "Update an offer")
    public ApiResponse<CatalogOfferDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCatalogOfferRequest request) {
        return ApiResponse.ok(catalogService.updateOffer(id, request));
    }

    @DeleteMapping("/offers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete (archive) an offer")
    public void delete(@PathVariable UUID id) {
        catalogService.deleteOffer(id);
    }
}
