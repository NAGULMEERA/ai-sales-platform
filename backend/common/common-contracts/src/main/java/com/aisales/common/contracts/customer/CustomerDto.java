package com.aisales.common.contracts.customer;

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
public class CustomerDto {

    private UUID id;
    private UUID tenantId;
    private UUID organizationId;
    private String fullName;
    private String phone;
    private String email;
    private CustomerStatus status;
    private CustomerSourceType sourceType;
    private UUID sourceLeadId;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
