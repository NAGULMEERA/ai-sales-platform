package com.aisales.lead.domain.service;

import com.aisales.common.contracts.lead.LeadStatus;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves allowed stage transitions for a sales pipeline.
 * Falls back to {@link DefaultSalesPipelineDefinition} when the pipeline has no rows.
 */
public interface PipelineTransitionSource {

    Set<LeadStatus> allowedTargets(UUID pipelineId, LeadStatus from);

    default void evict(UUID pipelineId) {
        // optional cache invalidation
    }
}
