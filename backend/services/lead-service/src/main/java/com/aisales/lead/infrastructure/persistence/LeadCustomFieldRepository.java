package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.LeadCustomFieldDefinition;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadCustomFieldRepository extends JpaRepository<LeadCustomFieldDefinition, UUID> {

    List<LeadCustomFieldDefinition> findByTenantIdOrderByDisplayOrderAscCreatedAtAsc(UUID tenantId);
}
