package com.aisales.common.contracts.ai;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptDto {

    private UUID id;
    private UUID tenantId;
    private UUID organizationId;
    private String code;
    private String name;
    private String purpose;
    private PromptStatus status;
    private Integer activeVersion;
    private PromptVersionDto latestVersion;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
