package com.aisales.lead.application.service;

import com.aisales.common.contracts.lead.EnsurePipelineRequest;
import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.contracts.lead.PipelineDto;
import com.aisales.common.contracts.lead.PipelineStageDto;
import com.aisales.common.contracts.lead.PipelineTransitionDto;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.lead.domain.entity.SalesPipeline;
import com.aisales.lead.domain.entity.SalesPipelineStage;
import com.aisales.lead.domain.entity.SalesPipelineTransition;
import com.aisales.lead.domain.service.DefaultSalesPipelineDefinition;
import com.aisales.lead.domain.service.PipelineTemplateDefinition;
import com.aisales.lead.domain.service.PipelineTemplateRegistry;
import com.aisales.lead.domain.service.PipelineTransitionSource;
import com.aisales.lead.infrastructure.persistence.SalesPipelineRepository;
import com.aisales.lead.infrastructure.persistence.SalesPipelineStageRepository;
import com.aisales.lead.infrastructure.persistence.SalesPipelineTransitionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PipelineService {

    private final SalesPipelineRepository pipelineRepository;
    private final SalesPipelineStageRepository stageRepository;
    private final SalesPipelineTransitionRepository transitionRepository;
    private final PipelineTransitionSource transitionSource;
    private final PipelineTemplateRegistry templateRegistry;

    @Transactional
    public SalesPipeline ensureDefaultPipeline(UUID tenantId) {
        return pipelineRepository.findByTenantIdAndDefaultPipelineTrueAndActiveTrue(tenantId)
                .or(() -> pipelineRepository.findByTenantIdAndCode(tenantId, DefaultSalesPipelineDefinition.CODE))
                .orElseGet(() -> createFromTemplate(tenantId, templateRegistry.require(DefaultSalesPipelineDefinition.CODE), true));
    }

    @Transactional
    public UUID resolvePipelineIdForCreate(UUID tenantId, UUID requestedPipelineId) {
        if (requestedPipelineId == null) {
            return ensureDefaultPipeline(tenantId).getId();
        }
        SalesPipeline pipeline = pipelineRepository.findByIdAndTenantId(requestedPipelineId, tenantId)
                .orElseThrow(() -> new NotFoundException("Pipeline not found: " + requestedPipelineId));
        if (!pipeline.isActive()) {
            throw new ValidationException("Pipeline is not active");
        }
        return pipeline.getId();
    }

    @Transactional(readOnly = true)
    public List<PipelineDto> listPipelines() {
        UUID tenantId = requireTenantId();
        List<SalesPipeline> pipelines =
                pipelineRepository.findByTenantIdAndActiveTrueOrderByNameAsc(tenantId);
        if (pipelines.isEmpty()) {
            return List.of();
        }
        List<UUID> pipelineIds = pipelines.stream().map(SalesPipeline::getId).toList();
        Map<UUID, List<SalesPipelineStage>> stagesByPipeline =
                stageRepository.findByPipelineIdInOrderByStageOrderAsc(pipelineIds).stream()
                        .collect(Collectors.groupingBy(SalesPipelineStage::getPipelineId));
        Map<UUID, List<SalesPipelineTransition>> transitionsByPipeline =
                transitionRepository.findByPipelineIdIn(pipelineIds).stream()
                        .collect(Collectors.groupingBy(SalesPipelineTransition::getPipelineId));
        return pipelines.stream()
                .map(pipeline -> toDto(
                        pipeline,
                        stagesByPipeline.getOrDefault(pipeline.getId(), List.of()),
                        transitionsByPipeline.getOrDefault(pipeline.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PipelineDto getPipeline(UUID pipelineId) {
        UUID tenantId = requireTenantId();
        SalesPipeline pipeline = pipelineRepository.findByIdAndTenantId(pipelineId, tenantId)
                .orElseThrow(() -> new NotFoundException("Pipeline not found: " + pipelineId));
        return toDto(pipeline);
    }

    @Transactional
    public PipelineDto getOrCreateDefaultPipeline() {
        UUID tenantId = requireTenantId();
        return toDto(ensureDefaultPipeline(tenantId));
    }

    /**
     * Idempotent ensure of a pipeline from a known template (same API for all industries).
     */
    @Transactional
    public PipelineDto ensurePipeline(EnsurePipelineRequest request) {
        UUID tenantId = requireTenantId();
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        PipelineTemplateDefinition template = templateRegistry.require(code);

        return pipelineRepository.findByTenantIdAndCode(tenantId, code)
                .map(this::toDto)
                .orElseGet(() -> {
                    boolean makeDefault = request.isMakeDefault()
                            && pipelineRepository.findByTenantIdAndDefaultPipelineTrueAndActiveTrue(tenantId).isEmpty();
                    return toDto(createFromTemplate(tenantId, template, makeDefault));
                });
    }

    private SalesPipeline createFromTemplate(
            UUID tenantId, PipelineTemplateDefinition template, boolean defaultPipeline) {
        Instant now = Instant.now();
        UUID actor = parseUuidOrNull(TenantContext.getUserId());

        SalesPipeline pipeline = pipelineRepository.saveAndFlush(SalesPipeline.builder()
                .tenantId(tenantId)
                .code(template.code())
                .name(template.name())
                .description(template.description())
                .active(true)
                .defaultPipeline(defaultPipeline)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor)
                .updatedBy(actor)
                .build());

        List<SalesPipelineStage> stages = new ArrayList<>();
        for (PipelineTemplateDefinition.StageSeed seed : template.stages()) {
            stages.add(SalesPipelineStage.builder()
                    .pipelineId(pipeline.getId())
                    .stageCode(seed.status().name())
                    .displayName(seed.displayName())
                    .stageOrder(seed.order())
                    .terminal(seed.terminal())
                    .build());
        }
        stageRepository.saveAll(stages);

        List<SalesPipelineTransition> transitions = new ArrayList<>();
        for (Map.Entry<LeadStatus, Set<LeadStatus>> entry : template.transitions().entrySet()) {
            for (LeadStatus to : entry.getValue()) {
                transitions.add(SalesPipelineTransition.builder()
                        .pipelineId(pipeline.getId())
                        .fromStage(entry.getKey().name())
                        .toStage(to.name())
                        .build());
            }
        }
        transitionRepository.saveAll(transitions);
        transitionSource.evict(pipeline.getId());
        return pipeline;
    }

    private PipelineDto toDto(SalesPipeline pipeline) {
        return toDto(
                pipeline,
                stageRepository.findByPipelineIdOrderByStageOrderAsc(pipeline.getId()),
                transitionRepository.findByPipelineId(pipeline.getId()));
    }

    private PipelineDto toDto(
            SalesPipeline pipeline,
            List<SalesPipelineStage> stageRows,
            List<SalesPipelineTransition> transitionRows) {
        List<PipelineStageDto> stages = stageRows.stream()
                .sorted(Comparator.comparingInt(SalesPipelineStage::getStageOrder))
                .map(stage -> PipelineStageDto.builder()
                        .id(stage.getId())
                        .stageCode(stage.getStageCode())
                        .displayName(stage.getDisplayName())
                        .stageOrder(stage.getStageOrder())
                        .terminal(stage.isTerminal())
                        .build())
                .toList();
        List<PipelineTransitionDto> transitions = transitionRows.stream()
                .map(row -> PipelineTransitionDto.builder()
                        .fromStage(row.getFromStage())
                        .toStage(row.getToStage())
                        .build())
                .toList();
        return PipelineDto.builder()
                .id(pipeline.getId())
                .tenantId(pipeline.getTenantId())
                .code(pipeline.getCode())
                .name(pipeline.getName())
                .description(pipeline.getDescription())
                .active(pipeline.isActive())
                .defaultPipeline(pipeline.isDefaultPipeline())
                .stages(stages)
                .transitions(transitions)
                .createdAt(pipeline.getCreatedAt())
                .updatedAt(pipeline.getUpdatedAt())
                .build();
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }

    private static UUID parseUuidOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
