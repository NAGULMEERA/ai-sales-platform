package com.aisales.common.contracts.search;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    @Size(max = 500)
    private String query;

    @Builder.Default
    private SearchEntityType entityType = SearchEntityType.ALL;

    @Builder.Default
    private SearchMode mode = SearchMode.HYBRID;

    @Builder.Default
    private Map<String, String> filters = new HashMap<>();

    @Size(max = 64)
    private String sortBy;

    @Builder.Default
    private boolean sortDesc = true;

    @Min(0)
    @Builder.Default
    private int page = 0;

    @Min(1)
    @Max(100)
    @Builder.Default
    private int size = 20;

    @Builder.Default
    private boolean includeFacets = true;

    @Builder.Default
    private boolean highlight = true;

    /** Optional knowledge base for KNOWLEDGE / SEMANTIC retrieval via AI Gateway. */
    private java.util.UUID knowledgeBaseId;
}
