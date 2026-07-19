package com.aisales.customer.infrastructure.persistence;

import com.aisales.customer.domain.entity.CustomerInteraction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerInteractionRepository extends JpaRepository<CustomerInteraction, UUID> {

    List<CustomerInteraction> findByTenantIdAndCustomerIdOrderByOccurredAtDesc(
            UUID tenantId, UUID customerId);

    @Modifying
    @Query("""
            UPDATE CustomerInteraction i
            SET i.customerId = :survivorId
            WHERE i.tenantId = :tenantId AND i.customerId = :loserId
            """)
    int reassignToSurvivor(
            @Param("tenantId") UUID tenantId,
            @Param("loserId") UUID loserId,
            @Param("survivorId") UUID survivorId);
}
