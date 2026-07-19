package com.aisales.customer.infrastructure.persistence;

import com.aisales.customer.domain.entity.CustomerConsent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerConsentRepository extends JpaRepository<CustomerConsent, UUID> {

    List<CustomerConsent> findByTenantIdAndCustomerIdOrderByGrantedAtDesc(UUID tenantId, UUID customerId);

    Optional<CustomerConsent> findByTenantIdAndCustomerIdAndConsentTypeAndConsentVersion(
            UUID tenantId, UUID customerId, String consentType, String consentVersion);

    @Modifying
    @Query("""
            UPDATE CustomerConsent c
            SET c.customerId = :survivorId
            WHERE c.tenantId = :tenantId AND c.customerId = :loserId
            """)
    int reassignToSurvivor(
            @Param("tenantId") UUID tenantId,
            @Param("loserId") UUID loserId,
            @Param("survivorId") UUID survivorId);
}
