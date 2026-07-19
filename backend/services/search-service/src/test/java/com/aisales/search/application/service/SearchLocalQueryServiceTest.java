package com.aisales.search.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.search.SearchEntityType;
import com.aisales.common.contracts.search.SearchMode;
import com.aisales.common.contracts.search.SearchResponse;
import com.aisales.search.infrastructure.persistence.SearchDocumentRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchLocalQueryServiceTest {

    @Mock
    private SearchDocumentRepository documentRepository;

    private SearchLocalQueryService service;

    @BeforeEach
    void setUp() {
        service = new SearchLocalQueryService(documentRepository);
    }

    @Test
    void shouldSkipHydrationWhenNoHits() {
        UUID tenantId = UUID.randomUUID();
        when(documentRepository.searchHybrid(eq(tenantId), eq("LEAD"), eq("xyz"), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(documentRepository.countHybrid(eq(tenantId), eq("LEAD"), eq("xyz"), any(), any()))
                .thenReturn(0L);

        SearchResponse response = service.searchLocal(
                tenantId, SearchMode.HYBRID, SearchEntityType.LEAD, 0, 10, "xyz", null, null, false, true);

        assertThat(response.getHits()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
        verify(documentRepository, never()).findAllByTenantAndIds(any(), any());
    }
}
