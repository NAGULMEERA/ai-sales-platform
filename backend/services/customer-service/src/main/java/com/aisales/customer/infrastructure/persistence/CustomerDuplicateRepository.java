package com.aisales.customer.infrastructure.persistence;

import com.aisales.customer.domain.entity.CustomerDuplicate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerDuplicateRepository extends JpaRepository<CustomerDuplicate, UUID> {

    List<CustomerDuplicate> findByTenantIdAndCustomerIdAndResolvedFalse(UUID tenantId, UUID customerId);

    Optional<CustomerDuplicate> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndCustomerIdAndDuplicateOfCustomerIdAndResolvedFalse(
            UUID tenantId, UUID customerId, UUID duplicateOfCustomerId);

    /**
     * Batch load of unresolved counterpart IDs for a customer (avoids per-match EXISTS).
     */
    @Query("""
            SELECT d.duplicateOfCustomerId FROM CustomerDuplicate d
            WHERE d.tenantId = :tenantId
              AND d.customerId = :customerId
              AND d.resolved = false
            """)
    List<UUID> findUnresolvedDuplicateOfCustomerIds(
            @Param("tenantId") UUID tenantId,
            @Param("customerId") UUID customerId);
}
