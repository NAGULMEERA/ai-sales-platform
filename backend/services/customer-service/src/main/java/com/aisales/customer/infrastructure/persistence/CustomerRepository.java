package com.aisales.customer.infrastructure.persistence;

import com.aisales.common.contracts.customer.CustomerStatus;
import com.aisales.customer.domain.entity.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    Optional<Customer> findByTenantIdAndSourceLeadIdAndDeletedAtIsNull(UUID tenantId, UUID sourceLeadId);

    @Query("""
            SELECT c FROM Customer c
            WHERE c.tenantId = :tenantId
              AND c.deletedAt IS NULL
              AND (:status IS NULL OR c.status = :status)
              AND (:q IS NULL OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(c.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR c.phone LIKE CONCAT('%', :q, '%'))
            """)
    Page<Customer> search(
            @Param("tenantId") UUID tenantId,
            @Param("status") CustomerStatus status,
            @Param("q") String q,
            Pageable pageable);
}
