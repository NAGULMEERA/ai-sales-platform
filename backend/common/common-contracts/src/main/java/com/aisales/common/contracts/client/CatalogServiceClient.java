package com.aisales.common.contracts.client;

import com.aisales.common.contracts.catalog.CatalogMatchRequest;
import com.aisales.common.contracts.catalog.CatalogMatchResultDto;
import com.aisales.common.contracts.catalog.CatalogOfferDto;
import com.aisales.common.contracts.catalog.CatalogProductDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "catalog-service",
        path = "/api/v1",
        url = "${aisales.clients.catalog-service.url:}")
public interface CatalogServiceClient {

    @GetMapping("/products/{id}")
    ApiResponse<CatalogProductDto> getProduct(@PathVariable UUID id);

    @GetMapping("/products")
    ApiResponse<PageResponse<CatalogProductDto>> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);

    @GetMapping("/offers/{id}")
    ApiResponse<CatalogOfferDto> getOffer(@PathVariable UUID id);

    @PostMapping("/matches")
    ApiResponse<CatalogMatchResultDto> match(@RequestBody CatalogMatchRequest request);
}
