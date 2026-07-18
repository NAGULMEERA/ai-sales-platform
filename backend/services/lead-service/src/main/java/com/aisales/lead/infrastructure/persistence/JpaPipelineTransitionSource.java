package com.aisales.lead.infrastructure.persistence;

import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.lead.domain.service.DefaultSalesPipelineDefinition;
import com.aisales.lead.domain.service.PipelineTransitionSource;
import com.aisales.lead.domain.entity.SalesPipelineTransition;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class JpaPipelineTransitionSource implements PipelineTransitionSource {

    private final SalesPipelineTransitionRepository transitionRepository;
    private final Map<UUID, Map<LeadStatus, Set<LeadStatus>>> cache = new ConcurrentHashMap<>();

    public JpaPipelineTransitionSource(SalesPipelineTransitionRepository transitionRepository) {
        this.transitionRepository = transitionRepository;
    }

    @Override
    public Set<LeadStatus> allowedTargets(UUID pipelineId, LeadStatus from) {
        if (pipelineId == null) {
            return DefaultSalesPipelineDefinition.allowedTargets(from);
        }
        Map<LeadStatus, Set<LeadStatus>> graph = cache.computeIfAbsent(pipelineId, this::loadGraph);
        if (graph.isEmpty()) {
            return DefaultSalesPipelineDefinition.allowedTargets(from);
        }
        return EnumSet.copyOf(graph.getOrDefault(from, EnumSet.noneOf(LeadStatus.class)));
    }

    public void evict(UUID pipelineId) {
        if (pipelineId != null) {
            cache.remove(pipelineId);
        }
    }

    private Map<LeadStatus, Set<LeadStatus>> loadGraph(UUID pipelineId) {
        List<SalesPipelineTransition> rows = transitionRepository.findByPipelineId(pipelineId);
        if (rows.isEmpty()) {
            return Map.of();
        }
        Map<LeadStatus, Set<LeadStatus>> graph = new java.util.EnumMap<>(LeadStatus.class);
        for (SalesPipelineTransition row : rows) {
            LeadStatus from = parseStatus(row.getFromStage());
            LeadStatus to = parseStatus(row.getToStage());
            if (from == null || to == null) {
                continue;
            }
            graph.computeIfAbsent(from, ignored -> EnumSet.noneOf(LeadStatus.class)).add(to);
        }
        return graph;
    }

    private static LeadStatus parseStatus(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return LeadStatus.valueOf(code.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
