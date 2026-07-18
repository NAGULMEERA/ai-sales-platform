package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.LeadStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadStatusHistoryRepository extends JpaRepository<LeadStatusHistory, UUID> {

    List<LeadStatusHistory> findByLeadIdOrderByCreatedAtDesc(UUID leadId);
}
