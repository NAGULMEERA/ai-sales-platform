package com.aisales.catalog.infrastructure.persistence;

import com.aisales.catalog.domain.entity.CatalogProduct;
import com.aisales.common.contracts.catalog.CatalogItemStatus;
import com.aisales.common.contracts.catalog.CatalogProductType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogProductRepository extends JpaRepository<CatalogProduct, UUID> {

    Optional<CatalogProduct> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    boolean existsByTenantIdAndCodeAndDeletedAtIsNull(UUID tenantId, String code);

    @Query("""
            SELECT p FROM CatalogProduct p
            WHERE p.tenantId = :tenantId
              AND p.deletedAt IS NULL
              AND (:status IS NULL OR p.status = :status)
              AND (:productType IS NULL OR p.productType = :productType)
              AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
              AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.code) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<CatalogProduct> search(
            @Param("tenantId") UUID tenantId,
            @Param("status") CatalogItemStatus status,
            @Param("productType") CatalogProductType productType,
            @Param("category") String category,
            @Param("q") String q,
            Pageable pageable);
}
