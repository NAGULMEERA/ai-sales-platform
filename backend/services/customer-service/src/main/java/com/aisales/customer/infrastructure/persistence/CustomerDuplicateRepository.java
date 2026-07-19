package com.aisales.customer.infrastructure.persistence;

import com.aisales.customer.domain.entity.CustomerDuplicate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerDuplicateRepository extends JpaRepository<CustomerDuplicate, UUID> {

    List<CustomerDuplicate> findByTenantIdAndCustomerIdAndResolvedFalse(UUID tenantId, UUID customerId);

    Optional<CustomerDuplicate> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndCustomerIdAndDuplicateOfCustomerIdAndResolvedFalse(
            UUID tenantId, UUID customerId, UUID duplicateOfCustomerId);
}
