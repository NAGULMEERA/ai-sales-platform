package com.aisales.search.application.service;

import com.aisales.common.contracts.search.SearchEntityType;
import com.aisales.common.contracts.search.SearchHitDto;
import com.aisales.common.contracts.search.SearchMode;
import com.aisales.common.contracts.search.SearchResponse;
import com.aisales.search.domain.entity.SearchDocument;
import com.aisales.search.infrastructure.persistence.SearchDocumentRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Short-lived read-only DB search. No remote calls — keeps JDBC connections free of AI latency.
 */
@Service
@RequiredArgsConstructor
public class SearchLocalQueryService {

    private final SearchDocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public SearchResponse searchLocal(
            UUID tenantId,
            SearchMode mode,
            SearchEntityType entityType,
            int page,
            int size,
            String query,
            String status,
            String source,
            boolean includeFacets,
            boolean highlight) {
        String entityTypeParam = entityType.name();
        List<UUID> ids = documentRepository.searchHybrid(
                tenantId, entityTypeParam, query, status, source, size, page * size);
        long total = documentRepository.countHybrid(
                tenantId, entityTypeParam, query, status, source);

        Map<UUID, SearchDocument> byId = ids.isEmpty()
                ? Collections.emptyMap()
                : documentRepository.findAllByTenantAndIds(tenantId, ids).stream()
                        .collect(Collectors.toMap(
                                SearchDocument::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        List<SearchHitDto> hits = new ArrayList<>();
        int rank = 0;
        for (UUID id : ids) {
            SearchDocument doc = byId.get(id);
            if (doc == null) {
                continue;
            }
            hits.add(toHit(doc, query, highlight, 1.0 / (++rank)));
        }

        Map<String, Map<String, Long>> facets = new HashMap<>();
        if (includeFacets) {
            facets.put("status", toFacetMap(documentRepository.facetStatus(tenantId, entityTypeParam)));
            facets.put("entityType", toFacetMap(documentRepository.facetEntityType(tenantId)));
            facets.put("source", toFacetMap(documentRepository.facetSource(tenantId, entityTypeParam)));
        }

        List<String> autocomplete = List.of();
        if (StringUtils.hasText(query) && query.length() >= 2) {
            autocomplete = documentRepository.autocomplete(tenantId, entityTypeParam, query, 5);
        }

        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return SearchResponse.builder()
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
    }

    private static SearchHitDto toHit(SearchDocument doc, String query, boolean highlight, double rankScore) {
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
}
