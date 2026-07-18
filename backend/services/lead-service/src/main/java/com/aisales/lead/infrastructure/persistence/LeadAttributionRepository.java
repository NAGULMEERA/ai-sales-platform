package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.LeadAttribution;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadAttributionRepository extends JpaRepository<LeadAttribution, UUID> {

    List<LeadAttribution> findByLeadIdOrderByCreatedAtDesc(UUID leadId);
}
