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
import com.aisales.common.contracts.lead.EnsurePipelineRequest;
import com.aisales.lead.domain.entity.SalesPipelineStage;
import com.aisales.lead.domain.service.DefaultSalesPipelineDefinition;
import com.aisales.lead.domain.service.PipelineTemplateDefinition;
import com.aisales.lead.domain.service.PipelineTemplateRegistry;
import com.aisales.lead.domain.service.PipelineTemplateTestSupport;
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

    private static final String RE_CODE = "REAL_ESTATE_SALES_V1";
    private static final String AUTO_CODE = "AUTOMOBILE_SALES_V1";

    private PipelineService pipelineService;
    private PipelineTemplateRegistry templateRegistry;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        TenantContext.setUserId(UUID.randomUUID().toString());
        templateRegistry = PipelineTemplateTestSupport.registry();
        pipelineService = new PipelineService(
                pipelineRepository, stageRepository, transitionRepository, transitionSource,
                templateRegistry);
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

    @Test
    void shouldEnsureRealEstatePipelineWithVisitNegotiationBookedLabels() {
        PipelineTemplateDefinition re = templateRegistry.require(RE_CODE);
        stubCreatePipeline();
        when(pipelineRepository.findByTenantIdAndCode(tenantId, RE_CODE))
                .thenReturn(Optional.empty());
        when(stageRepository.findByPipelineIdOrderByStageOrderAsc(any())).thenAnswer(inv -> {
            UUID pipelineId = inv.getArgument(0);
            return re.stages().stream()
                    .map(s -> SalesPipelineStage.builder()
                            .id(UUID.randomUUID())
                            .pipelineId(pipelineId)
                            .stageCode(s.status().name())
                            .displayName(s.displayName())
                            .stageOrder(s.order())
                            .terminal(s.terminal())
                            .build())
                    .toList();
        });

        PipelineDto dto = pipelineService.ensurePipeline(EnsurePipelineRequest.builder()
                .code(RE_CODE)
                .build());

        assertThat(dto.getCode()).isEqualTo(RE_CODE);
        assertThat(dto.getStages()).anySatisfy(s -> {
            assertThat(s.getStageCode()).isEqualTo("VISITED");
            assertThat(s.getDisplayName()).isEqualTo("Visit");
        });
        assertThat(dto.getStages()).anySatisfy(s -> {
            assertThat(s.getStageCode()).isEqualTo("NEGOTIATING");
            assertThat(s.getDisplayName()).isEqualTo("Negotiation");
        });
        assertThat(dto.getStages()).anySatisfy(s -> {
            assertThat(s.getStageCode()).isEqualTo("WON");
            assertThat(s.getDisplayName()).isEqualTo("Booked");
        });
        assertThat(re.transitions().get(LeadStatus.QUALIFIED))
                .containsExactlyInAnyOrder(LeadStatus.VISITED, LeadStatus.LOST);
        assertThat(re.transitions().get(LeadStatus.VISITED)).contains(LeadStatus.NEGOTIATING);
        assertThat(re.transitions().get(LeadStatus.NEGOTIATING)).contains(LeadStatus.WON);
    }

    @Test
    void shouldEnsureAutomobilePipelineWithTestDriveQuotationFinanceBookedLabels() {
        PipelineTemplateDefinition auto = templateRegistry.require(AUTO_CODE);
        stubCreatePipeline();
        when(pipelineRepository.findByTenantIdAndCode(tenantId, AUTO_CODE))
                .thenReturn(Optional.empty());
        when(stageRepository.findByPipelineIdOrderByStageOrderAsc(any())).thenAnswer(inv -> {
            UUID pipelineId = inv.getArgument(0);
            return auto.stages().stream()
                    .map(s -> SalesPipelineStage.builder()
                            .id(UUID.randomUUID())
                            .pipelineId(pipelineId)
                            .stageCode(s.status().name())
                            .displayName(s.displayName())
                            .stageOrder(s.order())
                            .terminal(s.terminal())
                            .build())
                    .toList();
        });

        PipelineDto dto = pipelineService.ensurePipeline(EnsurePipelineRequest.builder()
                .code(AUTO_CODE)
                .build());

        assertThat(dto.getCode()).isEqualTo(AUTO_CODE);
        assertThat(dto.getStages()).anySatisfy(s -> {
            assertThat(s.getStageCode()).isEqualTo("APPOINTMENT_BOOKED");
            assertThat(s.getDisplayName()).isEqualTo("Test Drive");
        });
        assertThat(dto.getStages()).anySatisfy(s -> {
            assertThat(s.getStageCode()).isEqualTo("VISITED");
            assertThat(s.getDisplayName()).isEqualTo("Quotation");
        });
        assertThat(dto.getStages()).anySatisfy(s -> {
            assertThat(s.getStageCode()).isEqualTo("NEGOTIATING");
            assertThat(s.getDisplayName()).isEqualTo("Finance");
        });
        assertThat(dto.getStages()).anySatisfy(s -> {
            assertThat(s.getStageCode()).isEqualTo("WON");
            assertThat(s.getDisplayName()).isEqualTo("Booked");
        });
        assertThat(auto.transitions().get(LeadStatus.QUALIFIED))
                .containsExactlyInAnyOrder(LeadStatus.APPOINTMENT_BOOKED, LeadStatus.LOST);
        assertThat(auto.transitions().get(LeadStatus.APPOINTMENT_BOOKED)).contains(LeadStatus.VISITED);
        assertThat(auto.transitions().get(LeadStatus.VISITED)).contains(LeadStatus.NEGOTIATING);
        assertThat(auto.transitions().get(LeadStatus.NEGOTIATING)).contains(LeadStatus.WON);
    }

    private void stubCreatePipeline() {
        when(pipelineRepository.saveAndFlush(any(SalesPipeline.class))).thenAnswer(inv -> {
            SalesPipeline p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(stageRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(transitionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(transitionRepository.findByPipelineId(any())).thenReturn(List.of());
    }
}
