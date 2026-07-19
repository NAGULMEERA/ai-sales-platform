package com.aisales.customer.infrastructure.persistence;

import com.aisales.common.contracts.customer.ContactMethodType;
import com.aisales.customer.domain.entity.CustomerContactMethod;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerContactMethodRepository extends JpaRepository<CustomerContactMethod, UUID> {

    List<CustomerContactMethod> findByTenantIdAndCustomerIdAndDeletedAtIsNullOrderByPrimaryMethodDescCreatedAtAsc(
            UUID tenantId, UUID customerId);

    Optional<CustomerContactMethod> findByTenantIdAndCustomerIdAndIdAndDeletedAtIsNull(
            UUID tenantId, UUID customerId, UUID id);

    @Modifying
    @Query("""
            UPDATE CustomerContactMethod c
            SET c.primaryMethod = false
            WHERE c.tenantId = :tenantId
              AND c.customerId = :customerId
              AND c.methodType = :methodType
              AND c.deletedAt IS NULL
            """)
    void clearPrimaryFlags(
            @Param("tenantId") UUID tenantId,
            @Param("customerId") UUID customerId,
            @Param("methodType") ContactMethodType methodType);

    @Modifying
    @Query("""
            UPDATE CustomerContactMethod c
            SET c.customerId = :survivorId
            WHERE c.tenantId = :tenantId
              AND c.customerId = :loserId
              AND c.deletedAt IS NULL
            """)
    int reassignToSurvivor(
            @Param("tenantId") UUID tenantId,
            @Param("loserId") UUID loserId,
            @Param("survivorId") UUID survivorId);
}
