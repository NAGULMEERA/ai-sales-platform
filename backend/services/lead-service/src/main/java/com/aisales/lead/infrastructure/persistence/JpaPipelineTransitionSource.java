package com.aisales.lead.infrastructure.persistence;

import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.core.util.TenantContext;
import com.aisales.lead.domain.entity.SalesPipelineTransition;
import com.aisales.lead.domain.service.DefaultSalesPipelineDefinition;
import com.aisales.lead.domain.service.PipelineTransitionSource;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JpaPipelineTransitionSource implements PipelineTransitionSource {

    private final SalesPipelineTransitionRepository transitionRepository;
    private final SalesPipelineRepository pipelineRepository;
    private final Map<CacheKey, Map<LeadStatus, Set<LeadStatus>>> cache = new ConcurrentHashMap<>();

    public JpaPipelineTransitionSource(
            SalesPipelineTransitionRepository transitionRepository,
            SalesPipelineRepository pipelineRepository) {
        this.transitionRepository = transitionRepository;
        this.pipelineRepository = pipelineRepository;
    }

    @Override
    public Set<LeadStatus> allowedTargets(UUID pipelineId, LeadStatus from) {
        if (pipelineId == null) {
            return DefaultSalesPipelineDefinition.allowedTargets(from);
        }
        Map<LeadStatus, Set<LeadStatus>> graph = cache.computeIfAbsent(cacheKey(pipelineId), this::loadGraph);
        if (graph.isEmpty()) {
            return DefaultSalesPipelineDefinition.allowedTargets(from);
        }
        return EnumSet.copyOf(graph.getOrDefault(from, EnumSet.noneOf(LeadStatus.class)));
    }

    @Override
    public void evict(UUID pipelineId) {
        if (pipelineId != null) {
            cache.remove(cacheKey(pipelineId));
        }
    }

    private Map<LeadStatus, Set<LeadStatus>> loadGraph(CacheKey key) {
        // Defense in depth: never cache transitions for a pipeline the current tenant does not own.
        if (key.tenantId() != null
                && pipelineRepository.findByIdAndTenantId(key.pipelineId(), key.tenantId()).isEmpty()) {
            return Map.of();
        }
        List<SalesPipelineTransition> rows = transitionRepository.findByPipelineId(key.pipelineId());
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

    private static CacheKey cacheKey(UUID pipelineId) {
        String raw = TenantContext.getTenantId();
        UUID tenantId = StringUtils.hasText(raw) ? UUID.fromString(raw) : null;
        return new CacheKey(tenantId, pipelineId);
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

    private record CacheKey(UUID tenantId, UUID pipelineId) {
    }
}
