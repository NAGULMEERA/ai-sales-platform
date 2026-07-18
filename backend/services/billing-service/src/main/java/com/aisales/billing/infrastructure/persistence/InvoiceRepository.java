package com.aisales.billing.infrastructure.persistence;

import com.aisales.billing.domain.entity.Invoice;
import com.aisales.common.contracts.billing.InvoiceStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    @EntityGraph(attributePaths = "lineItems")
    Optional<Invoice> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    boolean existsByTenantIdAndPeriodStartAndPeriodEndAndSourceAndDeletedAtIsNull(
            UUID tenantId, Instant periodStart, Instant periodEnd, String source);

    Page<Invoice> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Page<Invoice> findByTenantIdAndStatusAndDeletedAtIsNull(
            UUID tenantId, InvoiceStatus status, Pageable pageable);
}
