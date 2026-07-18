package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.LeadScoreRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadScoreRepository extends JpaRepository<LeadScoreRecord, UUID> {

    List<LeadScoreRecord> findByLeadIdOrderByScoredAtDesc(UUID leadId);
}
