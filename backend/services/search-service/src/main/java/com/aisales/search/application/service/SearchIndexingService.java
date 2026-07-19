package com.aisales.search.application.service;

import com.aisales.common.contracts.search.SearchEntityType;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.aisales.search.domain.entity.SearchDocument;
import com.aisales.search.infrastructure.persistence.SearchDocumentRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SearchIndexingService {

    private final SearchDocumentRepository documentRepository;
    private final ObjectProvider<PlatformMetrics> platformMetrics;

    @Transactional
    public void upsert(
            UUID tenantId,
            UUID organizationId,
            SearchEntityType entityType,
            UUID entityId,
            String title,
            String body,
            String keywords,
            String status,
            String source,
            Double businessScore,
            Long popularity,
            Instant sourceUpdatedAt,
            Map<String, Object> metadata) {
        Instant now = Instant.now();
        SearchDocument document = documentRepository
                .findByTenantIdAndEntityTypeAndEntityIdAndDeletedAtIsNull(tenantId, entityType, entityId)
                .orElseGet(() -> SearchDocument.builder()
                        .tenantId(tenantId)
                        .entityType(entityType)
                        .entityId(entityId)
                        .createdAt(now)
                        .createdBy("system")
                        .build());

        if (organizationId != null) {
            document.setOrganizationId(organizationId);
        }
        if (StringUtils.hasText(title)) {
            document.setTitle(title.trim());
        } else if (!StringUtils.hasText(document.getTitle())) {
            document.setTitle(entityType.name() + " " + entityId);
        }
        if (body != null) {
            document.setBody(body);
        } else if (document.getBody() == null) {
            document.setBody("");
        }
        if (keywords != null) {
            document.setKeywords(keywords);
        }
        if (status != null) {
            document.setStatus(status);
        }
        if (source != null) {
            document.setSource(source);
        }
        if (businessScore != null) {
            document.setBusinessScore(businessScore);
        } else if (document.getBusinessScore() == null) {
            document.setBusinessScore(0d);
        }
        if (popularity != null) {
            document.setPopularity(popularity);
        } else if (document.getPopularity() == null) {
            document.setPopularity(0L);
        }
        if (sourceUpdatedAt != null) {
            document.setSourceUpdatedAt(sourceUpdatedAt);
        } else if (document.getSourceUpdatedAt() == null) {
            document.setSourceUpdatedAt(now);
        }
        if (metadata != null) {
            Map<String, Object> merged = document.getMetadata() == null
                    ? new HashMap<>()
                    : new HashMap<>(document.getMetadata());
            merged.putAll(metadata);
            document.setMetadata(merged);
        } else if (document.getMetadata() == null) {
            document.setMetadata(new HashMap<>());
        }
        document.setDeletedAt(null);
        document.setDeletedBy(null);
        document.touch("system");
        if (document.getCreatedAt() == null) {
            document.setCreatedAt(now);
        }

        SearchDocument saved = documentRepository.saveAndFlush(document);
        documentRepository.refreshSearchVector(saved.getId());

        PlatformMetrics metrics = platformMetrics.getIfAvailable();
        if (metrics != null) {
            metrics.incrementBusinessMetric(
                    MetricNames.SEARCH_INDEXED,
                    tenantId.toString(),
                    "entity_type",
                    entityType.name());
        }
    }

    @Transactional
    public void delete(UUID tenantId, SearchEntityType entityType, UUID entityId) {
        documentRepository
                .findByTenantIdAndEntityTypeAndEntityIdAndDeletedAtIsNull(tenantId, entityType, entityId)
                .ifPresent(doc -> {
                    doc.softDelete("system");
                    documentRepository.save(doc);
                });
    }
}
