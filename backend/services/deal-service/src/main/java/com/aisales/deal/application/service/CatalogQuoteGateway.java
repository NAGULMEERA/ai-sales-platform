package com.aisales.deal.application.service;

import com.aisales.common.contracts.catalog.CatalogOfferDto;
import com.aisales.common.contracts.catalog.CatalogProductDto;
import com.aisales.common.contracts.client.CatalogServiceClient;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.exception.exception.ValidationException;
import java.util.UUID;
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
        try {
            ApiResponse<CatalogOfferDto> response = catalogServiceClient.getOffer(offerId);
            if (response == null || response.getData() == null) {
                throw new ValidationException("Catalog offer not found: " + offerId);
            }
            return response.getData();
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ValidationException("Unable to resolve catalog offer: " + offerId);
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
