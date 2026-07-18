package com.aisales.lead.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.exception.exception.ValidationException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Same {@link LeadStateMachine} engine; RE vs Auto transition graphs differ (metadata JSON).
 */
class IndustryPipelinePathTest {

    private static final String RE_CODE = "REAL_ESTATE_SALES_V1";
    private static final String AUTO_CODE = "AUTOMOBILE_SALES_V1";

    private final UUID rePipelineId = UUID.randomUUID();
    private final UUID autoPipelineId = UUID.randomUUID();

    private PipelineTemplateDefinition reTemplate;
    private PipelineTemplateDefinition autoTemplate;
    private LeadStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        PipelineTemplateRegistry registry = PipelineTemplateTestSupport.registry();
        reTemplate = registry.require(RE_CODE);
        autoTemplate = registry.require(AUTO_CODE);
        stateMachine = new LeadStateMachine(new FixedTransitionSource(Map.of(
                rePipelineId, reTemplate.transitions(),
                autoPipelineId, autoTemplate.transitions())));
    }

    @Test
    void realEstateHappyPathNewQualifiedVisitNegotiationBooked() {
        assertThatCode(() -> {
            stateMachine.assertTransition(rePipelineId, LeadStatus.NEW, LeadStatus.QUALIFIED);
            stateMachine.assertTransition(rePipelineId, LeadStatus.QUALIFIED, LeadStatus.VISITED);
            stateMachine.assertTransition(rePipelineId, LeadStatus.VISITED, LeadStatus.NEGOTIATING);
            stateMachine.assertTransition(rePipelineId, LeadStatus.NEGOTIATING, LeadStatus.WON);
        }).doesNotThrowAnyException();

        assertThatThrownBy(() ->
                stateMachine.assertTransition(rePipelineId, LeadStatus.QUALIFIED, LeadStatus.APPOINTMENT_BOOKED))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void automobileHappyPathNewQualifiedTestDriveQuotationFinanceBooked() {
        assertThatCode(() -> {
            stateMachine.assertTransition(autoPipelineId, LeadStatus.NEW, LeadStatus.QUALIFIED);
            stateMachine.assertTransition(autoPipelineId, LeadStatus.QUALIFIED, LeadStatus.APPOINTMENT_BOOKED);
            stateMachine.assertTransition(autoPipelineId, LeadStatus.APPOINTMENT_BOOKED, LeadStatus.VISITED);
            stateMachine.assertTransition(autoPipelineId, LeadStatus.VISITED, LeadStatus.NEGOTIATING);
            stateMachine.assertTransition(autoPipelineId, LeadStatus.NEGOTIATING, LeadStatus.WON);
        }).doesNotThrowAnyException();

        assertThatThrownBy(() ->
                stateMachine.assertTransition(autoPipelineId, LeadStatus.QUALIFIED, LeadStatus.VISITED))
                .isInstanceOf(ValidationException.class);

        assertThat(autoTemplate.stages()).anySatisfy(s ->
                assertThat(s.status() == LeadStatus.APPOINTMENT_BOOKED
                        && "Test Drive".equals(s.displayName())).isTrue());
        assertThat(autoTemplate.stages()).anySatisfy(s ->
                assertThat(s.status() == LeadStatus.VISITED
                        && "Quotation".equals(s.displayName())).isTrue());
        assertThat(autoTemplate.stages()).anySatisfy(s ->
                assertThat(s.status() == LeadStatus.NEGOTIATING
                        && "Finance".equals(s.displayName())).isTrue());
    }

    private static final class FixedTransitionSource implements PipelineTransitionSource {
        private final Map<UUID, Map<LeadStatus, Set<LeadStatus>>> graphs;

        private FixedTransitionSource(Map<UUID, Map<LeadStatus, Set<LeadStatus>>> graphs) {
            this.graphs = graphs;
        }

        @Override
        public Set<LeadStatus> allowedTargets(UUID pipelineId, LeadStatus from) {
            Map<LeadStatus, Set<LeadStatus>> graph = graphs.get(pipelineId);
            if (graph == null) {
                return DefaultSalesPipelineDefinition.allowedTargets(from);
            }
            return EnumSet.copyOf(graph.getOrDefault(from, EnumSet.noneOf(LeadStatus.class)));
        }
    }
}
