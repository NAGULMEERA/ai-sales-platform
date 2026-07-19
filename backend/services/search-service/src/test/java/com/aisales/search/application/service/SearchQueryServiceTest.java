package com.aisales.search.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.client.AiServiceClient;
import com.aisales.common.contracts.search.SearchEntityType;
import com.aisales.common.contracts.search.SearchHitDto;
import com.aisales.common.contracts.search.SearchMode;
import com.aisales.common.contracts.search.SearchRequest;
import com.aisales.common.contracts.search.SearchResponse;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.aisales.search.application.audit.SearchAuditor;
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
    @Mock
    private SearchLocalQueryService localQueryService;

    private SearchQueryService service;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        lenient().when(platformMetrics.getIfAvailable()).thenReturn(null);
        lenient().when(aiServiceClient.getIfAvailable()).thenReturn(null);
        when(searchAuditor.getIfAvailable()).thenReturn(auditor);
        service = new SearchQueryService(
                documentRepository, aiServiceClient, platformMetrics, searchAuditor, localQueryService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldDelegateHybridSearchToLocalQueryService() {
        SearchResponse expected = SearchResponse.builder()
                .query("villa")
                .mode(SearchMode.HYBRID)
                .entityType(SearchEntityType.LEAD)
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .hits(List.of(SearchHitDto.builder().title("Premium villa lead").build()))
                .build();
        when(localQueryService.searchLocal(
                eq(tenantId),
                eq(SearchMode.HYBRID),
                eq(SearchEntityType.LEAD),
                eq(0),
                eq(10),
                eq("villa"),
                isNull(),
                isNull(),
                eq(false),
                eq(true)))
                .thenReturn(expected);

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
        verify(auditor).searchExecuted("villa");
    }

    @Test
    void shouldFallbackSemanticWithoutKnowledgeBaseToRequestedEntityType() {
        SearchResponse fallback = SearchResponse.builder()
                .mode(SearchMode.FULL_TEXT)
                .entityType(SearchEntityType.LEAD)
                .totalElements(0)
                .hits(List.of())
                .build();
        when(localQueryService.searchLocal(
                any(), any(), eq(SearchEntityType.LEAD), anyInt(), anyInt(),
                any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(fallback);

        SearchResponse response = service.search(SearchRequest.builder()
                .query("apartment")
                .entityType(SearchEntityType.LEAD)
                .mode(SearchMode.SEMANTIC)
                .page(0)
                .size(10)
                .build());

        assertThat(response.getEntityType()).isEqualTo(SearchEntityType.LEAD);
        verify(localQueryService).searchLocal(
                eq(tenantId),
                eq(SearchMode.FULL_TEXT),
                eq(SearchEntityType.LEAD),
                eq(0),
                eq(10),
                eq("apartment"),
                isNull(),
                isNull(),
                anyBoolean(),
                anyBoolean());
    }
}
