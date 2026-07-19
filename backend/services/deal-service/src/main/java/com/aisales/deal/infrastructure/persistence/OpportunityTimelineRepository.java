package com.aisales.deal.infrastructure.persistence;

import com.aisales.deal.domain.entity.OpportunityTimelineEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpportunityTimelineRepository extends JpaRepository<OpportunityTimelineEntry, UUID> {

    List<OpportunityTimelineEntry> findByTenantIdAndOpportunityIdOrderByCreatedAtDesc(
            UUID tenantId, UUID opportunityId);
}
