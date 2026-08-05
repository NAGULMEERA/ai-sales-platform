package com.aisales.lead.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.core.util.TenantContext;
import com.aisales.lead.domain.entity.SalesPipeline;
import com.aisales.lead.domain.entity.SalesPipelineTransition;
import com.aisales.lead.domain.service.DefaultSalesPipelineDefinition;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaPipelineTransitionSourceTest {

    @Mock private SalesPipelineTransitionRepository transitionRepository;
    @Mock private SalesPipelineRepository pipelineRepository;

    private JpaPipelineTransitionSource source;
    private UUID tenantId;
    private UUID pipelineId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        pipelineId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        source = new JpaPipelineTransitionSource(transitionRepository, pipelineRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldNotLoadTransitionsForPipelineOwnedByAnotherTenant() {
        when(pipelineRepository.findByIdAndTenantId(pipelineId, tenantId)).thenReturn(Optional.empty());

        Set<LeadStatus> allowed = source.allowedTargets(pipelineId, LeadStatus.NEW);

        assertThat(allowed).isEqualTo(DefaultSalesPipelineDefinition.allowedTargets(LeadStatus.NEW));
        verify(transitionRepository, never()).findByPipelineId(any());
    }

    @Test
    void shouldCacheTransitionsUnderTenantScopedKey() {
        when(pipelineRepository.findByIdAndTenantId(pipelineId, tenantId))
                .thenReturn(Optional.of(SalesPipeline.builder().id(pipelineId).tenantId(tenantId).build()));
        when(transitionRepository.findByPipelineId(pipelineId)).thenReturn(List.of(
                SalesPipelineTransition.builder()
                        .pipelineId(pipelineId)
                        .fromStage("NEW")
                        .toStage("CONTACTED")
                        .build()));

        Set<LeadStatus> first = source.allowedTargets(pipelineId, LeadStatus.NEW);
        Set<LeadStatus> second = source.allowedTargets(pipelineId, LeadStatus.NEW);

        assertThat(first).containsExactly(LeadStatus.CONTACTED);
        assertThat(second).containsExactly(LeadStatus.CONTACTED);
        verify(transitionRepository).findByPipelineId(pipelineId);
    }
}
