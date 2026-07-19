package com.aisales.customer.infrastructure.persistence;

import com.aisales.customer.domain.entity.CustomerTimelineEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerTimelineRepository extends JpaRepository<CustomerTimelineEntry, UUID> {

    List<CustomerTimelineEntry> findByTenantIdAndCustomerIdOrderByOccurredAtDesc(
            UUID tenantId, UUID customerId);

    @Modifying
    @Query("""
            UPDATE CustomerTimelineEntry t
            SET t.customerId = :survivorId
            WHERE t.tenantId = :tenantId AND t.customerId = :loserId
            """)
    int reassignToSurvivor(
            @Param("tenantId") UUID tenantId,
            @Param("loserId") UUID loserId,
            @Param("survivorId") UUID survivorId);
}
