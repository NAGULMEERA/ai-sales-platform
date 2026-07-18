package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.LeadAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadAssignmentRepository extends JpaRepository<LeadAssignment, UUID> {

    Optional<LeadAssignment> findFirstByLeadIdAndUnassignedAtIsNullOrderByAssignedAtDesc(UUID leadId);

    List<LeadAssignment> findByLeadIdOrderByAssignedAtDesc(UUID leadId);
}
