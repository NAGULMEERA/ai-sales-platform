package com.aisales.common.contracts.search;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHitDto {

    private UUID documentId;
    private SearchEntityType entityType;
    private UUID entityId;
    private String title;
    private String snippet;
    private String highlightedTitle;
    private String highlightedBody;
    private Double score;
    private Double textScore;
    private Double vectorScore;
    private Double businessScore;
    private Instant sourceUpdatedAt;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
