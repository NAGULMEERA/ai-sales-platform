package com.aisales.lead.domain.service;

import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.exception.exception.ValidationException;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Explicit lead journey transitions. Integrations (email/WhatsApp/AI) never mutate status directly.
 * When a lead has a {@code pipelineId}, transitions are resolved from pipeline config;
 * otherwise the platform default sales graph is used.
 */
@Component
public class LeadStateMachine {

    private final PipelineTransitionSource transitionSource;

    /** Unit-test / bootstrap constructor — uses the default sales pipeline graph. */
    public LeadStateMachine() {
        this.transitionSource = (pipelineId, from) -> DefaultSalesPipelineDefinition.allowedTargets(from);
    }

    @Autowired
    public LeadStateMachine(PipelineTransitionSource transitionSource) {
        this.transitionSource = transitionSource;
    }

    public boolean isTerminal(LeadStatus status) {
        return status == LeadStatus.WON || status == LeadStatus.LOST || status == LeadStatus.ARCHIVED;
    }

    public boolean isClosed(LeadStatus status) {
        return status == LeadStatus.ARCHIVED;
    }

    public void assertTransition(LeadStatus from, LeadStatus to) {
        assertTransition(null, from, to);
    }

    public void assertTransition(UUID pipelineId, LeadStatus from, LeadStatus to) {
        if (from == to) {
            return;
        }
        Set<LeadStatus> allowed = transitionSource.allowedTargets(pipelineId, from);
        if (!allowed.contains(to)) {
            throw new ValidationException("Invalid lead status transition from " + from + " to " + to);
        }
    }
}
