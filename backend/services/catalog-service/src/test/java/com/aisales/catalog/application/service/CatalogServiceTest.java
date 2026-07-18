package com.aisales.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.catalog.application.mapper.CatalogMapper;
import com.aisales.catalog.domain.entity.CatalogOffer;
import com.aisales.catalog.domain.entity.CatalogProduct;
import com.aisales.catalog.infrastructure.persistence.CatalogOfferRepository;
import com.aisales.catalog.infrastructure.persistence.CatalogProductRepository;
import com.aisales.common.contracts.catalog.CatalogItemStatus;
import com.aisales.common.contracts.catalog.CatalogOfferDto;
import com.aisales.common.contracts.catalog.CatalogProductDto;
import com.aisales.common.contracts.catalog.CatalogProductType;
import com.aisales.common.contracts.catalog.CreateCatalogOfferRequest;
import com.aisales.common.contracts.catalog.CreateCatalogProductRequest;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.CatalogProductCreatedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock private CatalogProductRepository productRepository;
    @Mock private CatalogOfferRepository offerRepository;
    @Mock private EventPublisher eventPublisher;

    private CatalogService catalogService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        TenantContext.setUserId(UUID.randomUUID().toString());
        catalogService = new CatalogService(
                productRepository, offerRepository, new CatalogMapper(), eventPublisher);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateProductAndPublishEvent() {
        CreateCatalogProductRequest request = CreateCatalogProductRequest.builder()
                .code("SKU-100")
                .name("City Studio")
                .productType(CatalogProductType.PRODUCT)
                .category("residential")
                .build();

        when(productRepository.existsByTenantIdAndCodeAndDeletedAtIsNull(tenantId, "SKU-100"))
                .thenReturn(false);
        when(productRepository.saveAndFlush(any(CatalogProduct.class))).thenAnswer(inv -> {
            CatalogProduct p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        CatalogProductDto dto = catalogService.createProduct(request);

        assertThat(dto.getCode()).isEqualTo("SKU-100");
        assertThat(dto.getTenantId()).isEqualTo(tenantId);
        assertThat(dto.getStatus()).isEqualTo(CatalogItemStatus.ACTIVE);

        ArgumentCaptor<CatalogProductCreatedEvent> captor =
                ArgumentCaptor.forClass(CatalogProductCreatedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("CatalogProductCreated");
        assertThat(captor.getValue().getCode()).isEqualTo("SKU-100");
    }

    @Test
    void shouldRejectDuplicateProductCode() {
        when(productRepository.existsByTenantIdAndCodeAndDeletedAtIsNull(tenantId, "SKU-100"))
                .thenReturn(true);

        assertThatThrownBy(() -> catalogService.createProduct(CreateCatalogProductRequest.builder()
                        .code("SKU-100")
                        .name("Dup")
                        .productType(CatalogProductType.SERVICE)
                        .build()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldCreateOfferForProduct() {
        UUID productId = UUID.randomUUID();
        CatalogProduct product = CatalogProduct.builder()
                .id(productId)
                .tenantId(tenantId)
                .code("SKU-1")
                .name("Plan")
                .productType(CatalogProductType.PRODUCT)
                .status(CatalogItemStatus.ACTIVE)
                .build();
        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, productId))
                .thenReturn(Optional.of(product));
        when(offerRepository.existsByTenantIdAndCodeAndDeletedAtIsNull(tenantId, "OFFER-1"))
                .thenReturn(false);
        when(offerRepository.saveAndFlush(any(CatalogOffer.class))).thenAnswer(inv -> {
            CatalogOffer o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        CatalogOfferDto offer = catalogService.createOffer(CreateCatalogOfferRequest.builder()
                .productId(productId)
                .code("OFFER-1")
                .name("Launch price")
                .unitPrice(new BigDecimal("1000000.00"))
                .currency("INR")
                .build());

        assertThat(offer.getProductId()).isEqualTo(productId);
        assertThat(offer.getUnitPrice()).isEqualByComparingTo("1000000.00");
    }

    @Test
    void shouldNotExposeOtherTenantProduct() {
        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(eq(tenantId), any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getProduct(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }
}
