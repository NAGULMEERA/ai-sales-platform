package com.aisales.common.contracts.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

    private String query;
    private SearchMode mode;
    private SearchEntityType entityType;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    @Builder.Default
    private List<SearchHitDto> hits = new ArrayList<>();

    /** Facet field → value → count */
    @Builder.Default
    private Map<String, Map<String, Long>> facets = new HashMap<>();

    @Builder.Default
    private List<String> autocomplete = new ArrayList<>();
}
