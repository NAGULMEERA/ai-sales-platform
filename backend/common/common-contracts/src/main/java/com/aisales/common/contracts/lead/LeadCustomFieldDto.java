package com.aisales.common.contracts.lead;

import java.time.Instant;
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
public class LeadCustomFieldDto {
    private UUID id;
    private UUID tenantId;
    private String fieldName;
    private String fieldType;
    private Map<String, Object> fieldOptions;
    private boolean required;
    private Integer displayOrder;
    private Instant createdAt;
}
