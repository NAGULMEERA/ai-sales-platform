package com.aisales.search.application.service;

import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import com.aisales.common.contracts.client.AiServiceClient;
import com.aisales.common.contracts.ai.AiExecuteRequest;
import com.aisales.common.contracts.ai.AiExecuteResponse;
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
import com.aisales.search.domain.entity.SearchDocument;
import com.aisales.search.infrastructure.persistence.SearchDocumentRepository;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    @Transactional(readOnly = true)
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
            if (mode == SearchMode.SEMANTIC || entityType == SearchEntityType.KNOWLEDGE
                    && mode == SearchMode.VECTOR) {
                SearchResponse semantic = semanticKnowledge(request, tenantId, page, size);
                if (metrics != null) {
                    metrics.incrementBusinessMetric(
                            MetricNames.RAG_REQUEST, tenantId.toString(), "mode", mode.name());
                }
                recordMetrics(metrics, sample, tenantId, mode, true);
                return semantic;
            }

            String entityTypeParam = entityType.name();
            List<UUID> ids = documentRepository.searchHybrid(
                    tenantId, entityTypeParam, query, status, source, size, page * size);
            long total = documentRepository.countHybrid(
                    tenantId, entityTypeParam, query, status, source);

            Map<UUID, SearchDocument> byId = documentRepository.findAllByTenantAndIds(tenantId, ids).stream()
                    .collect(Collectors.toMap(SearchDocument::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

            List<SearchHitDto> hits = new ArrayList<>();
            int rank = 0;
            for (UUID id : ids) {
                SearchDocument doc = byId.get(id);
                if (doc == null) {
                    continue;
                }
                hits.add(toHit(doc, query, request.isHighlight(), 1.0 / (++rank)));
            }

            Map<String, Map<String, Long>> facets = new HashMap<>();
            if (request.isIncludeFacets()) {
                facets.put("status", toFacetMap(documentRepository.facetStatus(tenantId, entityTypeParam)));
                facets.put("entityType", toFacetMap(documentRepository.facetEntityType(tenantId)));
                facets.put("source", toFacetMap(documentRepository.facetSource(tenantId, entityTypeParam)));
            }

            List<String> autocomplete = List.of();
            if (StringUtils.hasText(query) && query.length() >= 2) {
                autocomplete = documentRepository.autocomplete(tenantId, entityTypeParam, query, 5);
            }

            int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
            SearchResponse response = SearchResponse.builder()
                    .query(query)
                    .mode(mode)
                    .entityType(entityType)
                    .page(page)
                    .size(size)
                    .totalElements(total)
                    .totalPages(totalPages)
                    .hits(hits)
                    .facets(facets)
                    .autocomplete(autocomplete)
                    .build();
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
        AiServiceClient client = aiServiceClient.getIfAvailable();
        if (client == null || request.getKnowledgeBaseId() == null || !StringUtils.hasText(request.getQuery())) {
            // Fall back to local FTS for knowledge projections
            SearchRequest fallback = SearchRequest.builder()
                    .query(request.getQuery())
                    .entityType(SearchEntityType.KNOWLEDGE)
                    .mode(SearchMode.FULL_TEXT)
                    .filters(request.getFilters())
                    .page(page)
                    .size(size)
                    .includeFacets(request.isIncludeFacets())
                    .highlight(request.isHighlight())
                    .build();
            return search(fallback);
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
            SearchRequest fallback = SearchRequest.builder()
                    .query(request.getQuery())
                    .entityType(SearchEntityType.KNOWLEDGE)
                    .mode(SearchMode.FULL_TEXT)
                    .page(page)
                    .size(size)
                    .includeFacets(false)
                    .highlight(true)
                    .build();
            return search(fallback);
        }
    }

    private SearchHitDto toHit(SearchDocument doc, String query, boolean highlight, double rankScore) {
        String snippet = snippet(doc.getBody(), query);
        return SearchHitDto.builder()
                .documentId(doc.getId())
                .entityType(doc.getEntityType())
                .entityId(doc.getEntityId())
                .title(doc.getTitle())
                .snippet(snippet)
                .highlightedTitle(highlight ? highlight(doc.getTitle(), query) : doc.getTitle())
                .highlightedBody(highlight ? highlight(snippet, query) : snippet)
                .score(rankScore)
                .textScore(rankScore)
                .businessScore(doc.getBusinessScore())
                .sourceUpdatedAt(doc.getSourceUpdatedAt())
                .metadata(doc.getMetadata() == null ? Map.of() : new HashMap<>(doc.getMetadata()))
                .build();
    }

    private static String snippet(String body, String query) {
        if (!StringUtils.hasText(body)) {
            return "";
        }
        if (!StringUtils.hasText(query)) {
            return body.length() <= 240 ? body : body.substring(0, 240) + "...";
        }
        String lower = body.toLowerCase(Locale.ROOT);
        String q = query.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(q);
        if (idx < 0) {
            return body.length() <= 240 ? body : body.substring(0, 240) + "...";
        }
        int start = Math.max(0, idx - 60);
        int end = Math.min(body.length(), idx + query.length() + 120);
        String slice = body.substring(start, end);
        return (start > 0 ? "..." : "") + slice + (end < body.length() ? "..." : "");
    }

    private static String highlight(String text, String query) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(query)) {
            return text;
        }
        String pattern = "(?i)(" + java.util.regex.Pattern.quote(query.trim()) + ")";
        return text.replaceAll(pattern, "<em>$1</em>");
    }

    private static Map<String, Long> toFacetMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        if (rows == null) {
            return map;
        }
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] != null) {
                map.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
            }
        }
        return map;
    }

    private static String filterValue(Map<String, String> filters, String key) {
        if (filters == null || !filters.containsKey(key)) {
            return null;
        }
        return trimToNull(filters.get(key));
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

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
