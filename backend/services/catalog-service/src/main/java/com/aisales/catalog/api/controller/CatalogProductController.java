package com.aisales.catalog.api.controller;

import com.aisales.catalog.application.service.CatalogService;
import com.aisales.common.contracts.catalog.CatalogItemStatus;
import com.aisales.common.contracts.catalog.CatalogProductDto;
import com.aisales.common.contracts.catalog.CatalogProductType;
import com.aisales.common.contracts.catalog.CreateCatalogProductRequest;
import com.aisales.common.contracts.catalog.UpdateCatalogProductRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Catalog Products", description = "Industry-agnostic products and services")
public class CatalogProductController {

    private final CatalogService catalogService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a catalog product or service")
    public ApiResponse<CatalogProductDto> create(@Valid @RequestBody CreateCatalogProductRequest request) {
        return ApiResponse.ok(catalogService.createProduct(request));
    }

    @GetMapping
    @Operation(summary = "List / search catalog products")
    public ApiResponse<PageResponse<CatalogProductDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) CatalogItemStatus status,
            @RequestParam(required = false) CatalogProductType productType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q) {
        return ApiResponse.ok(catalogService.listProducts(page, size, status, productType, category, q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a catalog product")
    public ApiResponse<CatalogProductDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(catalogService.getProduct(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a catalog product")
    public ApiResponse<CatalogProductDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCatalogProductRequest request) {
        return ApiResponse.ok(catalogService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete (archive) a catalog product")
    public void delete(@PathVariable UUID id) {
        catalogService.deleteProduct(id);
    }
}
