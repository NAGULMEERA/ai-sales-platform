package com.aisales.deal.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.common.contracts.deal.OpportunityStatus;
import com.aisales.common.exception.exception.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OpportunityLifecycleTest {

    @Test
    void shouldFollowQualifiedProposalNegotiationPath() {
        Opportunity opportunity = base(OpportunityStatus.OPEN);
        UUID actor = UUID.randomUUID();

        opportunity.transitionTo(OpportunityStatus.QUALIFIED, actor);
        opportunity.transitionTo(OpportunityStatus.QUOTED, actor);
        opportunity.transitionTo(OpportunityStatus.NEGOTIATION, actor);
        opportunity.closeWon("signed", actor);

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.WON);
        assertThat(opportunity.getCloseReason()).isEqualTo("signed");
        assertThat(opportunity.getProbability()).isEqualTo(100);
    }

    @Test
    void shouldReopenLostOpportunity() {
        Opportunity opportunity = base(OpportunityStatus.LOST);
        opportunity.reopen(OpportunityStatus.QUALIFIED, UUID.randomUUID());
        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.QUALIFIED);
        assertThat(opportunity.getCloseReason()).isNull();
    }

    @Test
    void shouldRejectInvalidTransitionFromQuotedToOpen() {
        Opportunity opportunity = base(OpportunityStatus.QUOTED);
        assertThatThrownBy(() -> opportunity.transitionTo(OpportunityStatus.OPEN, UUID.randomUUID()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void shouldAppendNotes() {
        Opportunity opportunity = base(OpportunityStatus.OPEN);
        UUID actor = UUID.randomUUID();
        opportunity.addNote("First", actor);
        opportunity.addNote("Second", actor);
        assertThat(opportunity.getNotes()).contains("First").contains("Second");
    }

    private static Opportunity base(OpportunityStatus status) {
        return Opportunity.builder()
                .id(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .name("Deal")
                .currency("INR")
                .status(status)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
