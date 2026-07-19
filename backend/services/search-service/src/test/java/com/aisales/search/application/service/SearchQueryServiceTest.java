package com.aisales.search.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.client.AiServiceClient;
import com.aisales.common.contracts.search.SearchEntityType;
import com.aisales.common.contracts.search.SearchMode;
import com.aisales.common.contracts.search.SearchRequest;
import com.aisales.common.contracts.search.SearchResponse;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.aisales.search.application.audit.SearchAuditor;
import com.aisales.search.domain.entity.SearchDocument;
import com.aisales.search.infrastructure.persistence.SearchDocumentRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class SearchQueryServiceTest {

    @Mock
    private SearchDocumentRepository documentRepository;
    @Mock
    private ObjectProvider<AiServiceClient> aiServiceClient;
    @Mock
    private ObjectProvider<PlatformMetrics> platformMetrics;
    @Mock
    private ObjectProvider<SearchAuditor> searchAuditor;
    @Mock
    private SearchAuditor auditor;

    private SearchQueryService service;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        lenient().when(platformMetrics.getIfAvailable()).thenReturn(null);
        lenient().when(aiServiceClient.getIfAvailable()).thenReturn(null);
        when(searchAuditor.getIfAvailable()).thenReturn(auditor);
        service = new SearchQueryService(documentRepository, aiServiceClient, platformMetrics, searchAuditor);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldSearchHybridAndReturnHits() {
        UUID docId = UUID.randomUUID();
        SearchDocument doc = SearchDocument.builder()
                .id(docId)
                .tenantId(tenantId)
                .entityType(SearchEntityType.LEAD)
                .entityId(UUID.randomUUID())
                .title("Premium villa lead")
                .body("Looking for 3BHK villa")
                .businessScore(70d)
                .build();

        when(documentRepository.searchHybrid(eq(tenantId), eq("LEAD"), eq("villa"), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(docId));
        when(documentRepository.countHybrid(eq(tenantId), eq("LEAD"), eq("villa"), any(), any()))
                .thenReturn(1L);
        when(documentRepository.findAllByTenantAndIds(eq(tenantId), eq(List.of(docId))))
                .thenReturn(List.of(doc));
        when(documentRepository.autocomplete(eq(tenantId), eq("LEAD"), eq("villa"), eq(5)))
                .thenReturn(List.of("Premium villa lead"));

        SearchResponse response = service.search(SearchRequest.builder()
                .query("villa")
                .entityType(SearchEntityType.LEAD)
                .mode(SearchMode.HYBRID)
                .page(0)
                .size(10)
                .includeFacets(false)
                .highlight(true)
                .build());

        assertThat(response.getTotalElements()).isEqualTo(1L);
        assertThat(response.getHits()).hasSize(1);
        assertThat(response.getHits().getFirst().getTitle()).isEqualTo("Premium villa lead");
        assertThat(response.getHits().getFirst().getHighlightedTitle()).contains("<em>");
        assertThat(response.getAutocomplete()).contains("Premium villa lead");
    }
}
