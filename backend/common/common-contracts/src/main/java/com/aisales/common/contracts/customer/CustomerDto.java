package com.aisales.common.contracts.customer;

import java.time.LocalDate;
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
    private String customerNumber;
    private String fullName;
    private String phone;
    private String email;
    private String whatsapp;
    private String externalCrmId;
    private String governmentId;
    private String gender;
    private LocalDate dateOfBirth;
    private String language;
    private String preferredChannel;
    private CustomerStatus status;
    private CustomerSourceType sourceType;
    private UUID sourceLeadId;
    private UUID mergedIntoCustomerId;
    private Map<String, Object> preferences;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
