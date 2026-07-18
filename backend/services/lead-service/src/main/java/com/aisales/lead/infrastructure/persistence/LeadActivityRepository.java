package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.LeadActivity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadActivityRepository extends JpaRepository<LeadActivity, UUID> {

    List<LeadActivity> findByLeadIdOrderByCreatedAtDesc(UUID leadId);
}
