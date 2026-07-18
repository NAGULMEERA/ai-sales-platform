package com.aisales.lead.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.exception.exception.ValidationException;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LeadStateMachineTest {

    @Test
    void defaultConstructorUsesLegacySalesGraph() {
        LeadStateMachine machine = new LeadStateMachine();
        assertThatCode(() -> machine.assertTransition(LeadStatus.NEW, LeadStatus.CONTACTED))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> machine.assertTransition(LeadStatus.NEW, LeadStatus.WON))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void pipelineSourceCanRestrictTransitions() {
        UUID pipelineId = UUID.randomUUID();
        PipelineTransitionSource source = (id, from) -> {
            if (pipelineId.equals(id) && from == LeadStatus.NEW) {
                return EnumSet.of(LeadStatus.LOST);
            }
            return Set.of();
        };
        LeadStateMachine machine = new LeadStateMachine(source);

        assertThatCode(() -> machine.assertTransition(pipelineId, LeadStatus.NEW, LeadStatus.LOST))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> machine.assertTransition(pipelineId, LeadStatus.NEW, LeadStatus.CONTACTED))
                .isInstanceOf(ValidationException.class);
    }
}
