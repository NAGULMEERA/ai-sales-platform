package com.aisales.customer.infrastructure.persistence;

import com.aisales.customer.domain.entity.CustomerAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {

    List<CustomerAddress> findByTenantIdAndCustomerIdAndDeletedAtIsNullOrderByPrimaryAddressDescCreatedAtAsc(
            UUID tenantId, UUID customerId);

    Optional<CustomerAddress> findByTenantIdAndCustomerIdAndIdAndDeletedAtIsNull(
            UUID tenantId, UUID customerId, UUID id);

    @Modifying
    @Query("""
            UPDATE CustomerAddress a
            SET a.primaryAddress = false, a.updatedAt = CURRENT_TIMESTAMP
            WHERE a.tenantId = :tenantId
              AND a.customerId = :customerId
              AND a.deletedAt IS NULL
              AND a.primaryAddress = true
            """)
    void clearPrimaryFlags(@Param("tenantId") UUID tenantId, @Param("customerId") UUID customerId);

    @Modifying
    @Query("""
            UPDATE CustomerAddress a
            SET a.customerId = :survivorId, a.updatedAt = CURRENT_TIMESTAMP
            WHERE a.tenantId = :tenantId
              AND a.customerId = :loserId
              AND a.deletedAt IS NULL
            """)
    int reassignToSurvivor(
            @Param("tenantId") UUID tenantId,
            @Param("loserId") UUID loserId,
            @Param("survivorId") UUID survivorId);
}
