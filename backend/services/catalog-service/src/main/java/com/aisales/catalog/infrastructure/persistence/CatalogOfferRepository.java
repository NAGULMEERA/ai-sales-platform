package com.aisales.catalog.infrastructure.persistence;

import com.aisales.catalog.domain.entity.CatalogOffer;
import com.aisales.common.contracts.catalog.CatalogItemStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogOfferRepository extends JpaRepository<CatalogOffer, UUID> {

    Optional<CatalogOffer> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    List<CatalogOffer> findByTenantIdAndIdInAndDeletedAtIsNull(UUID tenantId, List<UUID> ids);

    boolean existsByTenantIdAndCodeAndDeletedAtIsNull(UUID tenantId, String code);

    List<CatalogOffer> findByTenantIdAndProductIdAndDeletedAtIsNull(UUID tenantId, UUID productId);

    @Query("""
            SELECT o FROM CatalogOffer o
            WHERE o.tenantId = :tenantId
              AND o.deletedAt IS NULL
              AND (:productId IS NULL OR o.productId = :productId)
              AND (:status IS NULL OR o.status = :status)
            """)
    Page<CatalogOffer> search(
            @Param("tenantId") UUID tenantId,
            @Param("productId") UUID productId,
            @Param("status") CatalogItemStatus status,
            Pageable pageable);

    @Query("""
            SELECT o FROM CatalogOffer o
            WHERE o.tenantId = :tenantId
              AND o.deletedAt IS NULL
              AND o.status = com.aisales.common.contracts.catalog.CatalogItemStatus.ACTIVE
              AND o.productId IN :productIds
            """)
    List<CatalogOffer> findActiveByTenantAndProductIds(
            @Param("tenantId") UUID tenantId,
            @Param("productIds") List<UUID> productIds);
}
