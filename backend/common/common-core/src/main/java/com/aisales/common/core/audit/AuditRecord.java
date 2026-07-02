package com.aisales.common.core.audit;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class AuditRecord {

    UUID tenantId;
    String userId;
    String action;
    String resourceType;
    String resourceId;
    String correlationId;
    String detailsJson;
}
