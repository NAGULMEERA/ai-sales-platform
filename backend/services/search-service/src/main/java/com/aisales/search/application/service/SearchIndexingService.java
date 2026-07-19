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

        document.setOrganizationId(organizationId);
        document.setTitle(StringUtils.hasText(title) ? title.trim() : entityType.name() + " " + entityId);
        document.setBody(body == null ? "" : body);
        document.setKeywords(keywords);
        document.setStatus(status);
        document.setSource(source);
        document.setBusinessScore(businessScore == null ? 0d : businessScore);
        document.setPopularity(popularity == null ? 0L : popularity);
        document.setSourceUpdatedAt(sourceUpdatedAt == null ? now : sourceUpdatedAt);
        document.setMetadata(metadata == null ? new HashMap<>() : new HashMap<>(metadata));
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
