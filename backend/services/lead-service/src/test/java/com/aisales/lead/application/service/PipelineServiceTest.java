package com.aisales.lead.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.contracts.lead.PipelineDto;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.lead.domain.entity.SalesPipeline;
import com.aisales.lead.domain.entity.SalesPipelineTransition;
import com.aisales.lead.domain.service.DefaultSalesPipelineDefinition;
import com.aisales.lead.domain.service.PipelineTransitionSource;
import com.aisales.lead.infrastructure.persistence.SalesPipelineRepository;
import com.aisales.lead.infrastructure.persistence.SalesPipelineStageRepository;
import com.aisales.lead.infrastructure.persistence.SalesPipelineTransitionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {

    @Mock private SalesPipelineRepository pipelineRepository;
    @Mock private SalesPipelineStageRepository stageRepository;
    @Mock private SalesPipelineTransitionRepository transitionRepository;
    @Mock private PipelineTransitionSource transitionSource;

    private PipelineService pipelineService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        TenantContext.setUserId(UUID.randomUUID().toString());
        pipelineService = new PipelineService(
                pipelineRepository, stageRepository, transitionRepository, transitionSource);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateDefaultSalesPipelineWithLegacyTransitions() {
        when(pipelineRepository.findByTenantIdAndDefaultPipelineTrueAndActiveTrue(tenantId))
                .thenReturn(Optional.empty());
        when(pipelineRepository.findByTenantIdAndCode(tenantId, DefaultSalesPipelineDefinition.CODE))
                .thenReturn(Optional.empty());
        when(pipelineRepository.saveAndFlush(any(SalesPipeline.class))).thenAnswer(inv -> {
            SalesPipeline p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(stageRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(transitionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(stageRepository.findByPipelineIdOrderByStageOrderAsc(any())).thenReturn(List.of());
        when(transitionRepository.findByPipelineId(any())).thenReturn(List.of());

        PipelineDto dto = pipelineService.getOrCreateDefaultPipeline();

        assertThat(dto.getCode()).isEqualTo(DefaultSalesPipelineDefinition.CODE);
        assertThat(dto.isDefaultPipeline()).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SalesPipelineTransition>> transitionCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(transitionRepository).saveAll(transitionCaptor.capture());
        int expectedTransitions = DefaultSalesPipelineDefinition.transitions().values().stream()
                .mapToInt(Set::size)
                .sum();
        assertThat(transitionCaptor.getValue()).hasSize(expectedTransitions);
        verify(transitionSource).evict(any(UUID.class));
    }

    @Test
    void shouldResolveRequestedPipelineForSameTenant() {
        UUID pipelineId = UUID.randomUUID();
        SalesPipeline pipeline = SalesPipeline.builder()
                .id(pipelineId)
                .tenantId(tenantId)
                .code("CUSTOM")
                .name("Custom")
                .active(true)
                .defaultPipeline(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(pipelineRepository.findByIdAndTenantId(pipelineId, tenantId)).thenReturn(Optional.of(pipeline));

        UUID resolved = pipelineService.resolvePipelineIdForCreate(tenantId, pipelineId);
        assertThat(resolved).isEqualTo(pipelineId);
    }

    @Test
    void shouldRejectPipelineFromOtherTenant() {
        UUID pipelineId = UUID.randomUUID();
        when(pipelineRepository.findByIdAndTenantId(pipelineId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pipelineService.resolvePipelineIdForCreate(tenantId, pipelineId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void defaultGraphAllowsNewToContacted() {
        assertThat(DefaultSalesPipelineDefinition.allowedTargets(LeadStatus.NEW))
                .contains(LeadStatus.CONTACTED, LeadStatus.QUALIFIED, LeadStatus.LOST);
        assertThat(DefaultSalesPipelineDefinition.allowedTargets(LeadStatus.ARCHIVED)).isEmpty();
    }
}
