package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.LeadDuplicate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadDuplicateRepository extends JpaRepository<LeadDuplicate, UUID> {

    List<LeadDuplicate> findByTenantIdAndResolvedOrderByDetectedAtDesc(UUID tenantId, boolean resolved);

    Optional<LeadDuplicate> findByTenantIdAndId(UUID tenantId, UUID id);
}
