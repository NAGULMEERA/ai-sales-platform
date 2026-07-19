package com.aisales.deal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.catalog.CatalogOfferDto;
import com.aisales.common.contracts.catalog.CatalogOfferLookupRequest;
import com.aisales.common.contracts.client.CatalogServiceClient;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.exception.exception.ValidationException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogQuoteGatewayTest {

    @Mock private CatalogServiceClient catalogServiceClient;

    private CatalogQuoteGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new CatalogQuoteGateway(catalogServiceClient);
    }

    @Test
    void shouldBatchLookupOffersInSingleFeignCall() {
        UUID offerA = UUID.randomUUID();
        UUID offerB = UUID.randomUUID();
        CatalogOfferDto dtoA = CatalogOfferDto.builder()
                .id(offerA)
                .code("A")
                .unitPrice(new BigDecimal("10"))
                .build();
        CatalogOfferDto dtoB = CatalogOfferDto.builder()
                .id(offerB)
                .code("B")
                .unitPrice(new BigDecimal("20"))
                .build();
        when(catalogServiceClient.lookupOffers(any(CatalogOfferLookupRequest.class)))
                .thenReturn(ApiResponse.ok(List.of(dtoA, dtoB)));

        Map<UUID, CatalogOfferDto> result = gateway.requireOffers(List.of(offerA, offerB, offerA));

        assertThat(result).containsOnlyKeys(offerA, offerB);
        ArgumentCaptor<CatalogOfferLookupRequest> captor =
                ArgumentCaptor.forClass(CatalogOfferLookupRequest.class);
        verify(catalogServiceClient).lookupOffers(captor.capture());
        assertThat(captor.getValue().getOfferIds()).containsExactlyInAnyOrder(offerA, offerB);
    }

    @Test
    void shouldReturnEmptyMapForEmptyOfferIds() {
        assertThat(gateway.requireOffers(List.of())).isEmpty();
        assertThat(gateway.requireOffers(null)).isEmpty();
    }

    @Test
    void shouldFailRequireOfferWhenMissingFromBatch() {
        UUID offerId = UUID.randomUUID();
        when(catalogServiceClient.lookupOffers(any(CatalogOfferLookupRequest.class)))
                .thenReturn(ApiResponse.ok(List.of()));

        assertThatThrownBy(() -> gateway.requireOffer(offerId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(offerId.toString());
    }

    @Test
    void shouldWrapFeignFailuresAsValidation() {
        when(catalogServiceClient.lookupOffers(any(CatalogOfferLookupRequest.class)))
                .thenThrow(new RuntimeException("downstream down"));

        assertThatThrownBy(() -> gateway.requireOffers(List.of(UUID.randomUUID())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unable to resolve catalog offers");
    }
}
