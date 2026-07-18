package com.aisales.common.contracts.lead;

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
public class LeadDto {

    private UUID id;
    private UUID tenantId;
    private UUID organizationId;
    private UUID pipelineId;
    private UUID customerId;
    private String externalId;
    private String customerName;
    private String phone;
    private String email;
    private String sourceType;
    private String sourceId;
    private String campaign;
    private String utmSource;
    private String utmCampaign;
    private LeadStatus status;
    private boolean validated;
    private boolean qualified;
    private Integer score;
    private Integer confidenceScore;
    private UUID assignedTo;
    /** Industry-agnostic attributes (plugin-defined keys). */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
