package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.LeadQualityScore;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadQualityScoreRepository extends JpaRepository<LeadQualityScore, UUID> {

    List<LeadQualityScore> findByLeadIdOrderByScoredAtDesc(UUID leadId);

    Optional<LeadQualityScore> findFirstByLeadIdOrderByScoredAtDesc(UUID leadId);
}
