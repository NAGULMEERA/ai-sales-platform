package com.aisales.deal.infrastructure.persistence;

import com.aisales.common.contracts.deal.OpportunityStatus;
import com.aisales.deal.domain.entity.Opportunity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {

    Optional<Opportunity> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    @Query("""
            SELECT o FROM Opportunity o
            WHERE o.tenantId = :tenantId
              AND o.deletedAt IS NULL
              AND (:status IS NULL OR o.status = :status)
              AND (:customerId IS NULL OR o.customerId = :customerId)
              AND (:leadId IS NULL OR o.leadId = :leadId)
            ORDER BY o.updatedAt DESC
            """)
    Page<Opportunity> search(
            @Param("tenantId") UUID tenantId,
            @Param("status") OpportunityStatus status,
            @Param("customerId") UUID customerId,
            @Param("leadId") UUID leadId,
            Pageable pageable);
}
