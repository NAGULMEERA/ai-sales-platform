package com.aisales.search.application.service;

import com.aisales.common.contracts.ai.AiExecuteRequest;
import com.aisales.common.contracts.ai.AiExecuteResponse;
import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import com.aisales.common.contracts.client.AiServiceClient;
import com.aisales.common.contracts.search.AutocompleteRequest;
import com.aisales.common.contracts.search.SearchEntityType;
import com.aisales.common.contracts.search.SearchHitDto;
import com.aisales.common.contracts.search.SearchMode;
import com.aisales.common.contracts.search.SearchRequest;
import com.aisales.common.contracts.search.SearchResponse;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.aisales.search.application.audit.SearchAuditor;
import com.aisales.search.infrastructure.persistence.SearchDocumentRepository;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchQueryService {

    private final SearchDocumentRepository documentRepository;
    private final ObjectProvider<AiServiceClient> aiServiceClient;
    private final ObjectProvider<PlatformMetrics> platformMetrics;
    private final ObjectProvider<SearchAuditor> searchAuditor;
    private final SearchLocalQueryService localQueryService;

    /**
     * Orchestrates search. Remote AI calls stay outside any DB transaction.
     */
    public SearchResponse search(SearchRequest request) {
        UUID tenantId = requireTenantId();
        SearchMode mode = request.getMode() == null ? SearchMode.HYBRID : request.getMode();
        SearchEntityType entityType =
                request.getEntityType() == null ? SearchEntityType.ALL : request.getEntityType();
        int page = Math.max(request.getPage(), 0);
        int size = Math.min(Math.max(request.getSize(), 1), 100);
        String query = trimToNull(request.getQuery());
        String status = filterValue(request.getFilters(), "status");
        String source = filterValue(request.getFilters(), "source");

        SearchAuditor auditor = searchAuditor.getIfAvailable();
        if (auditor != null) {
            auditor.searchExecuted(query == null ? "" : query);
        }

        PlatformMetrics metrics = platformMetrics.getIfAvailable();
        Timer.Sample sample = metrics == null ? null : metrics.startTimer();
        try {
            boolean useSemantic =
                    mode == SearchMode.SEMANTIC
                            || (mode == SearchMode.VECTOR && entityType == SearchEntityType.KNOWLEDGE);
            if (useSemantic) {
                SearchResponse semantic = semanticKnowledge(request, tenantId, page, size);
                if (metrics != null) {
                    metrics.incrementBusinessMetric(
                            MetricNames.RAG_REQUEST, tenantId.toString(), "mode", mode.name());
                }
                recordMetrics(metrics, sample, tenantId, mode, true);
                return semantic;
            }

            SearchResponse response = localQueryService.searchLocal(
                    tenantId, mode, entityType, page, size, query, status, source,
                    request.isIncludeFacets(), request.isHighlight());
            recordMetrics(metrics, sample, tenantId, mode, true);
            return response;
        } catch (RuntimeException ex) {
            recordMetrics(metrics, sample, tenantId, mode, false);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<String> autocomplete(AutocompleteRequest request) {
        UUID tenantId = requireTenantId();
        SearchEntityType entityType =
                request.getEntityType() == null ? SearchEntityType.ALL : request.getEntityType();
        return documentRepository.autocomplete(
                tenantId,
                entityType.name(),
                request.getPrefix().trim(),
                Math.min(Math.max(request.getLimit(), 1), 20));
    }

    private SearchResponse semanticKnowledge(
            SearchRequest request, UUID tenantId, int page, int size) {
        SearchEntityType fallbackType = request.getEntityType() == null
                ? SearchEntityType.KNOWLEDGE
                : request.getEntityType();
        AiServiceClient client = aiServiceClient.getIfAvailable();
        if (client == null || request.getKnowledgeBaseId() == null || !StringUtils.hasText(request.getQuery())) {
            return localQueryService.searchLocal(
                    tenantId,
                    SearchMode.FULL_TEXT,
                    fallbackType,
                    page,
                    size,
                    trimToNull(request.getQuery()),
                    filterValue(request.getFilters(), "status"),
                    filterValue(request.getFilters(), "source"),
                    request.isIncludeFacets(),
                    request.isHighlight());
        }
        try {
            ApiResponse<AiExecuteResponse> response = client.execute(AiExecuteRequest.builder()
                    .promptCode("SEARCH_SEMANTIC_V1")
                    .capability("SEARCH")
                    .knowledgeBaseId(request.getKnowledgeBaseId())
                    .retrievalQuery(request.getQuery())
                    .retrievalTopK(size)
                    .businessReference(tenantId.toString())
                    .variables(Map.of("question", request.getQuery()))
                    .build());
            List<SearchHitDto> hits = new ArrayList<>();
            if (response != null && response.getData() != null && response.getData().getRetrievedChunks() != null) {
                for (RetrievedKnowledgeChunkDto chunk : response.getData().getRetrievedChunks()) {
                    hits.add(SearchHitDto.builder()
                            .documentId(chunk.getChunkId())
                            .entityType(SearchEntityType.KNOWLEDGE)
                            .entityId(chunk.getDocumentId())
                            .title("Knowledge chunk " + chunk.getChunkIndex())
                            .snippet(chunk.getContent())
                            .score(chunk.getScore() != null
                                    ? chunk.getScore()
                                    : (chunk.getDistance() == null ? null : 1.0 - chunk.getDistance()))
                            .vectorScore(chunk.getDistance() == null ? null : 1.0 - chunk.getDistance())
                            .metadata(Map.of("chunkIndex", chunk.getChunkIndex()))
                            .build());
                }
            }
            return SearchResponse.builder()
                    .query(request.getQuery())
                    .mode(SearchMode.SEMANTIC)
                    .entityType(SearchEntityType.KNOWLEDGE)
                    .page(page)
                    .size(size)
                    .totalElements(hits.size())
                    .totalPages(1)
                    .hits(hits)
                    .build();
        } catch (Exception ex) {
            log.warn("Semantic knowledge search fell back to FTS: {}", ex.getMessage());
            return localQueryService.searchLocal(
                    tenantId,
                    SearchMode.FULL_TEXT,
                    fallbackType,
                    page,
                    size,
                    trimToNull(request.getQuery()),
                    null,
                    null,
                    false,
                    true);
        }
    }

    private void recordMetrics(
            PlatformMetrics metrics, Timer.Sample sample, UUID tenantId, SearchMode mode, boolean success) {
        if (metrics == null) {
            return;
        }
        metrics.incrementBusinessMetric(
                MetricNames.SEARCH_REQUEST,
                tenantId.toString(),
                "mode",
                mode.name(),
                "success",
                String.valueOf(success));
        if (sample != null) {
            metrics.recordTimer(sample, MetricNames.SEARCH_DURATION, "mode", mode.name());
        }
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }

    private static String filterValue(Map<String, String> filters, String key) {
        if (filters == null || !filters.containsKey(key)) {
            return null;
        }
        return trimToNull(filters.get(key));
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
