package com.aisales.search.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.search.SearchEntityType;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.aisales.search.domain.entity.SearchDocument;
import com.aisales.search.infrastructure.persistence.SearchDocumentRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class SearchIndexingServiceTest {

    @Mock
    private SearchDocumentRepository documentRepository;
    @Mock
    private ObjectProvider<PlatformMetrics> platformMetrics;

    private SearchIndexingService service;

    @BeforeEach
    void setUp() {
        when(platformMetrics.getIfAvailable()).thenReturn(null);
        when(documentRepository.findByTenantIdAndEntityTypeAndEntityIdAndDeletedAtIsNull(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(documentRepository.saveAndFlush(any(SearchDocument.class))).thenAnswer(inv -> {
            SearchDocument doc = inv.getArgument(0);
            if (doc.getId() == null) {
                doc.setId(UUID.randomUUID());
            }
            return doc;
        });
        service = new SearchIndexingService(documentRepository, platformMetrics);
    }

    @Test
    void shouldUpsertSearchDocumentAndRefreshVector() {
        UUID tenantId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();

        service.upsert(
                tenantId,
                null,
                SearchEntityType.LEAD,
                entityId,
                "Jane Lead",
                "Interested in villa",
                "WEB",
                "NEW",
                "WEB",
                80d,
                3L,
                Instant.now(),
                Map.of("status", "NEW"));

        ArgumentCaptor<SearchDocument> captor = ArgumentCaptor.forClass(SearchDocument.class);
        verify(documentRepository).saveAndFlush(captor.capture());
        SearchDocument saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getEntityType()).isEqualTo(SearchEntityType.LEAD);
        assertThat(saved.getTitle()).isEqualTo("Jane Lead");
        assertThat(saved.getBusinessScore()).isEqualTo(80d);
        verify(documentRepository).refreshSearchVector(saved.getId());
    }
}
