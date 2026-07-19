package com.aisales.lead.domain.service;

import com.aisales.lead.domain.entity.Lead;
import java.util.Optional;
import java.util.UUID;

/**
 * Extension point for {@link com.aisales.common.contracts.lead.AssignmentStrategy#AI}.
 * Industry/capability plugins may register an implementation; platform default has none.
 */
public interface LeadAiAssigneeResolver {

    Optional<UUID> suggestAssignee(UUID tenantId, Lead lead);
}
