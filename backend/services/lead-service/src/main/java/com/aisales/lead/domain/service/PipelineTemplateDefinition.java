package com.aisales.lead.domain.service;

import com.aisales.common.contracts.lead.LeadStatus;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bootstrap template for a tenant sales pipeline.
 * Stage codes remain {@link LeadStatus}; industries differ by display names and transition edges.
 */
public interface PipelineTemplateDefinition {

    String code();

    String name();

    String description();

    List<StageSeed> stages();

    Map<LeadStatus, Set<LeadStatus>> transitions();

    record StageSeed(LeadStatus status, String displayName, int order, boolean terminal) {
    }
}
