package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.LeadTagMapping;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadTagMappingRepository extends JpaRepository<LeadTagMapping, UUID> {

    List<LeadTagMapping> findByLeadIdOrderByAssignedAtDesc(UUID leadId);

    Optional<LeadTagMapping> findByLeadIdAndTagId(UUID leadId, UUID tagId);

    void deleteByLeadIdAndTagId(UUID leadId, UUID tagId);

    boolean existsByLeadIdAndTagId(UUID leadId, UUID tagId);
}
