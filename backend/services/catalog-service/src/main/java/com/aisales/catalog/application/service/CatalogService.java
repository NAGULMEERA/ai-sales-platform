package com.aisales.catalog.application.service;

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
import com.aisales.common.contracts.catalog.UpdateCatalogOfferRequest;
import com.aisales.common.contracts.catalog.UpdateCatalogProductRequest;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.CatalogOfferCreatedEvent;
import com.aisales.common.events.model.CatalogProductCreatedEvent;
import com.aisales.common.events.model.CatalogProductUpdatedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CatalogProductRepository productRepository;
    private final CatalogOfferRepository offerRepository;
    private final CatalogMapper mapper;
    private final EventPublisher eventPublisher;
    private final ObjectProvider<PlatformMetrics> platformMetrics;

    @Transactional
    public CatalogProductDto createProduct(CreateCatalogProductRequest request) {
        UUID tenantId = requireTenantId();
        UUID actor = actorId();
        Instant now = Instant.now();
        String code = request.getCode().trim();

        if (productRepository.existsByTenantIdAndCodeAndDeletedAtIsNull(tenantId, code)) {
            throw new ValidationException("Product code already exists: " + code);
        }

        CatalogProduct product = CatalogProduct.builder()
                .tenantId(tenantId)
                .organizationId(parseUuidOrNull(TenantContext.getOrganizationId()))
                .code(code)
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .productType(request.getProductType())
                .category(trimToNull(request.getCategory()))
                .status(request.getStatus() != null ? request.getStatus() : CatalogItemStatus.ACTIVE)
                .attributes(request.getAttributes() != null
                        ? new HashMap<>(request.getAttributes())
                        : new HashMap<>())
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        CatalogProduct saved = productRepository.saveAndFlush(product);
        eventPublisher.publish(CatalogProductCreatedEvent.of(
                tenantId.toString(),
                saved.getId().toString(),
                saved.getCode(),
                saved.getName(),
                saved.getProductType().name(),
                saved.getStatus().name(),
                correlationId()));
        incrementMetric(MetricNames.CATALOG_PRODUCT_CREATED, tenantId);
        return mapper.toProductDto(saved);
    }

    @Transactional(readOnly = true)
    public CatalogProductDto getProduct(UUID productId) {
        return mapper.toProductDto(requireProduct(productId));
    }

    @Transactional(readOnly = true)
    public PageResponse<CatalogProductDto> listProducts(
            int page, int size, CatalogItemStatus status, CatalogProductType productType,
            String category, String q) {
        UUID tenantId = requireTenantId();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<CatalogProduct> result = productRepository.search(
                tenantId, status, productType, trimToNull(category), trimToNull(q),
                PageRequest.of(safePage, safeSize));
        return PageResponse.<CatalogProductDto>builder()
                .content(result.getContent().stream().map(mapper::toProductDto).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    @Transactional
    public CatalogProductDto updateProduct(UUID productId, UpdateCatalogProductRequest request) {
        CatalogProduct product = requireProduct(productId);
        product.update(
                request.getName(),
                request.getDescription(),
                request.getProductType(),
                request.getCategory(),
                request.getStatus(),
                request.getAttributes(),
                actorId());
        CatalogProduct saved = productRepository.save(product);
        eventPublisher.publish(CatalogProductUpdatedEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                saved.getCode(),
                saved.getName(),
                saved.getProductType().name(),
                saved.getStatus().name(),
                correlationId()));
        incrementMetric(MetricNames.CATALOG_PRODUCT_UPDATED, saved.getTenantId());
        return mapper.toProductDto(saved);
    }

    @Transactional
    public void deleteProduct(UUID productId) {
        CatalogProduct product = requireProduct(productId);
        product.softDelete(actorId());
        productRepository.save(product);
    }

    @Transactional
    public CatalogOfferDto createOffer(CreateCatalogOfferRequest request) {
        UUID tenantId = requireTenantId();
        UUID actor = actorId();
        Instant now = Instant.now();
        CatalogProduct product = requireProduct(request.getProductId());
        String code = request.getCode().trim();

        if (offerRepository.existsByTenantIdAndCodeAndDeletedAtIsNull(tenantId, code)) {
            throw new ValidationException("Offer code already exists: " + code);
        }
        if (product.getStatus() == CatalogItemStatus.ARCHIVED) {
            throw new ValidationException("Cannot create offer for archived product");
        }

        String currency = StringUtils.hasText(request.getCurrency())
                ? request.getCurrency().trim().toUpperCase()
                : "INR";

        CatalogOffer offer = CatalogOffer.builder()
                .tenantId(tenantId)
                .productId(product.getId())
                .code(code)
                .name(request.getName().trim())
                .currency(currency)
                .unitPrice(request.getUnitPrice())
                .status(request.getStatus() != null ? request.getStatus() : CatalogItemStatus.ACTIVE)
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        CatalogOffer saved = offerRepository.saveAndFlush(offer);
        eventPublisher.publish(CatalogOfferCreatedEvent.of(
                tenantId.toString(),
                saved.getId().toString(),
                saved.getProductId().toString(),
                saved.getCode(),
                saved.getCurrency(),
                saved.getUnitPrice().toPlainString(),
                saved.getStatus().name(),
                correlationId()));
        return mapper.toOfferDto(saved);
    }

    @Transactional(readOnly = true)
    public CatalogOfferDto getOffer(UUID offerId) {
        return mapper.toOfferDto(requireOffer(offerId));
    }

    /**
     * Batch resolve offers by id (tenant-scoped). Missing ids are omitted from the result;
     * callers decide whether absence is an error.
     */
    @Transactional(readOnly = true)
    public List<CatalogOfferDto> getOffersByIds(List<UUID> offerIds) {
        if (offerIds == null || offerIds.isEmpty()) {
            return List.of();
        }
        List<UUID> distinct = offerIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return List.of();
        }
        if (distinct.size() > 100) {
            throw new ValidationException("At most 100 offer ids per lookup");
        }
        return offerRepository.findByTenantIdAndIdInAndDeletedAtIsNull(requireTenantId(), distinct)
                .stream()
                .map(mapper::toOfferDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<CatalogOfferDto> listOffers(
            int page, int size, UUID productId, CatalogItemStatus status) {
        UUID tenantId = requireTenantId();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<CatalogOffer> result = offerRepository.search(
                tenantId, productId, status, PageRequest.of(safePage, safeSize));
        return PageResponse.<CatalogOfferDto>builder()
                .content(result.getContent().stream().map(mapper::toOfferDto).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CatalogOfferDto> listOffersForProduct(UUID productId) {
        requireProduct(productId);
        return offerRepository.findByTenantIdAndProductIdAndDeletedAtIsNull(requireTenantId(), productId)
                .stream()
                .map(mapper::toOfferDto)
                .toList();
    }

    @Transactional
    public CatalogOfferDto updateOffer(UUID offerId, UpdateCatalogOfferRequest request) {
        CatalogOffer offer = requireOffer(offerId);
        offer.update(
                request.getName(),
                request.getCurrency(),
                request.getUnitPrice(),
                request.getStatus(),
                request.getValidFrom() != null ? request.getValidFrom() : offer.getValidFrom(),
                request.getValidTo() != null ? request.getValidTo() : offer.getValidTo(),
                actorId());
        return mapper.toOfferDto(offerRepository.save(offer));
    }

    @Transactional
    public void deleteOffer(UUID offerId) {
        CatalogOffer offer = requireOffer(offerId);
        offer.softDelete(actorId());
        offerRepository.save(offer);
    }

    CatalogProduct requireProduct(UUID productId) {
        return productRepository.findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));
    }

    private CatalogOffer requireOffer(UUID offerId) {
        return offerRepository.findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), offerId)
                .orElseThrow(() -> new NotFoundException("Offer not found: " + offerId));
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }

    private UUID actorId() {
        return parseUuidOrNull(TenantContext.getUserId());
    }

    private String correlationId() {
        return CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate);
    }

    private static UUID parseUuidOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void incrementMetric(String name, UUID tenantId) {
        PlatformMetrics metrics = platformMetrics.getIfAvailable();
        if (metrics != null) {
            metrics.incrementForTenant(name, tenantId.toString());
        }
    }
}
