package com.aisales.deal.application.service;

import com.aisales.common.contracts.catalog.CatalogOfferDto;
import com.aisales.common.contracts.catalog.CatalogOfferLookupRequest;
import com.aisales.common.contracts.catalog.CatalogProductDto;
import com.aisales.common.contracts.client.CatalogServiceClient;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.exception.exception.ValidationException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves catalog product/offer snapshots for quote line items.
 * Catalog remains system of record for pricing; deal-service stores immutable snapshots.
 */
@Component
@RequiredArgsConstructor
public class CatalogQuoteGateway {

    private final CatalogServiceClient catalogServiceClient;

    public CatalogOfferDto requireOffer(UUID offerId) {
        Map<UUID, CatalogOfferDto> found = requireOffers(List.of(offerId));
        CatalogOfferDto offer = found.get(offerId);
        if (offer == null) {
            throw new ValidationException("Catalog offer not found: " + offerId);
        }
        return offer;
    }

    /**
     * Batch resolve offers in one Feign call. Missing ids are absent from the map.
     */
    public Map<UUID, CatalogOfferDto> requireOffers(Collection<UUID> offerIds) {
        if (offerIds == null || offerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UUID> distinct = offerIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            ApiResponse<List<CatalogOfferDto>> response = catalogServiceClient.lookupOffers(
                    CatalogOfferLookupRequest.builder().offerIds(distinct).build());
            if (response == null || response.getData() == null) {
                return Collections.emptyMap();
            }
            return response.getData().stream()
                    .filter(Objects::nonNull)
                    .filter(o -> o.getId() != null)
                    .collect(Collectors.toMap(
                            CatalogOfferDto::getId,
                            Function.identity(),
                            (a, b) -> a,
                            HashMap::new));
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Unable to resolve catalog offers");
        }
    }

    public CatalogProductDto requireProduct(UUID productId) {
        try {
            ApiResponse<CatalogProductDto> response = catalogServiceClient.getProduct(productId);
            if (response == null || response.getData() == null) {
                throw new ValidationException("Catalog product not found: " + productId);
            }
            return response.getData();
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Unable to resolve catalog product: " + productId);
        }
    }
}
