package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.LeadNote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadNoteRepository extends JpaRepository<LeadNote, UUID> {

    List<LeadNote> findByLeadIdOrderByCreatedAtDesc(UUID leadId);

    Optional<LeadNote> findByIdAndLeadId(UUID id, UUID leadId);
}
