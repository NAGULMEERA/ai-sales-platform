package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.LeadTag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadTagRepository extends JpaRepository<LeadTag, UUID> {

    Optional<LeadTag> findByTenantIdAndTagIgnoreCase(UUID tenantId, String tag);

    List<LeadTag> findByTenantIdOrderByTagAsc(UUID tenantId);

    List<LeadTag> findByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);
}
