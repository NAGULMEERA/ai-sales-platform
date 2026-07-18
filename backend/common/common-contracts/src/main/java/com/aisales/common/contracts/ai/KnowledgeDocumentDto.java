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
public class KnowledgeDocumentDto {

    private UUID id;
    private UUID tenantId;
    private UUID knowledgeBaseId;
    private String name;
    private String contentType;
    private Long sizeBytes;
    /** Media-service reference (binary stays in S3 / media-service). */
    private UUID mediaId;
    private String objectKey;
    private KnowledgeDocumentStatus status;
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    private Instant createdAt;
    private Instant updatedAt;
}
