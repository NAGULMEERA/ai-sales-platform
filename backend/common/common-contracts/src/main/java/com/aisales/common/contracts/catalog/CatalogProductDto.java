package com.aisales.common.contracts.catalog;

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
public class CatalogProductDto {

    private UUID id;
    private UUID tenantId;
    private UUID organizationId;
    private String code;
    private String name;
    private String description;
    private CatalogProductType productType;
    private String category;
    private CatalogItemStatus status;
    private Map<String, Object> attributes;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
