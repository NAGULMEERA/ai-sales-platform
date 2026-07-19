package com.aisales.common.contracts.customer;

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
public class CustomerContactMethodDto {

    private UUID id;
    private UUID customerId;
    private ContactMethodType methodType;
    private String value;
    private String label;
    private boolean verified;
    private Instant verifiedAt;
    private boolean primary;
    private Instant createdAt;
}
