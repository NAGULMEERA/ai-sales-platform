package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.LeadAttachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadAttachmentRepository extends JpaRepository<LeadAttachment, UUID> {

    List<LeadAttachment> findByLeadIdOrderByUploadedAtDesc(UUID leadId);
}
