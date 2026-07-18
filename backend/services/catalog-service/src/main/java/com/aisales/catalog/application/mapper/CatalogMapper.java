package com.aisales.catalog.application.mapper;

import com.aisales.catalog.domain.entity.CatalogOffer;
import com.aisales.catalog.domain.entity.CatalogProduct;
import com.aisales.common.contracts.catalog.CatalogOfferDto;
import com.aisales.common.contracts.catalog.CatalogProductDto;
import java.util.HashMap;
import org.springframework.stereotype.Component;

@Component
public class CatalogMapper {

    public CatalogProductDto toProductDto(CatalogProduct product) {
        return CatalogProductDto.builder()
                .id(product.getId())
                .tenantId(product.getTenantId())
                .organizationId(product.getOrganizationId())
                .code(product.getCode())
                .name(product.getName())
                .description(product.getDescription())
                .productType(product.getProductType())
                .category(product.getCategory())
                .status(product.getStatus())
                .attributes(product.getAttributes() == null
                        ? new HashMap<>()
                        : new HashMap<>(product.getAttributes()))
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .version(product.getVersion())
                .build();
    }

    public CatalogOfferDto toOfferDto(CatalogOffer offer) {
        return CatalogOfferDto.builder()
                .id(offer.getId())
                .tenantId(offer.getTenantId())
                .productId(offer.getProductId())
                .code(offer.getCode())
                .name(offer.getName())
                .currency(offer.getCurrency())
                .unitPrice(offer.getUnitPrice())
                .status(offer.getStatus())
                .validFrom(offer.getValidFrom())
                .validTo(offer.getValidTo())
                .createdAt(offer.getCreatedAt())
                .updatedAt(offer.getUpdatedAt())
                .version(offer.getVersion())
                .build();
    }
}
