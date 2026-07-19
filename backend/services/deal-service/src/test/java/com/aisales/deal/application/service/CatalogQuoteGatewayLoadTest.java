package com.aisales.deal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.catalog.CatalogOfferDto;
import com.aisales.common.contracts.catalog.CatalogOfferLookupRequest;
import com.aisales.common.contracts.client.CatalogServiceClient;
import com.aisales.common.core.dto.ApiResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Concurrent batch offer resolution smoke test (not a full soak). Tags allow CI to split suites.
 */
@Tag("performance")
@Tag("load")
@ExtendWith(MockitoExtension.class)
class CatalogQuoteGatewayLoadTest {

    @Mock private CatalogServiceClient catalogServiceClient;

    private CatalogQuoteGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new CatalogQuoteGateway(catalogServiceClient);
    }

    @Test
    void shouldHandleConcurrentBatchLookups() throws Exception {
        UUID offerId = UUID.randomUUID();
        CatalogOfferDto offer = CatalogOfferDto.builder()
                .id(offerId)
                .code("LOAD")
                .unitPrice(new BigDecimal("1"))
                .build();
        when(catalogServiceClient.lookupOffers(any(CatalogOfferLookupRequest.class)))
                .thenReturn(ApiResponse.ok(List.of(offer)));

        int threads = 24;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<Map<UUID, CatalogOfferDto>>> tasks = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                tasks.add(() -> gateway.requireOffers(List.of(offerId)));
            }
            List<Future<Map<UUID, CatalogOfferDto>>> futures = pool.invokeAll(tasks, 10, TimeUnit.SECONDS);
            for (Future<Map<UUID, CatalogOfferDto>> future : futures) {
                assertThat(future.get()).containsKey(offerId);
            }
        } finally {
            pool.shutdownNow();
        }

        verify(catalogServiceClient, times(threads)).lookupOffers(any(CatalogOfferLookupRequest.class));
    }
}
