package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.LeadFollowup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadFollowupRepository extends JpaRepository<LeadFollowup, UUID> {

    List<LeadFollowup> findByLeadIdOrderByScheduledAtAsc(UUID leadId);
}
