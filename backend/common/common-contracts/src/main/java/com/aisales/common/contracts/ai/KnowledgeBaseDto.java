package com.aisales.common.contracts.ai;

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
public class KnowledgeBaseDto {

    private UUID id;
    private UUID tenantId;
    private UUID organizationId;
    private String code;
    private String name;
    private String description;
    private KnowledgeBaseStatus status;
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
