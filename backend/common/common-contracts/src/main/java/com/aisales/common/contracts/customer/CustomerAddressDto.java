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
public class CustomerAddressDto {

    private UUID id;
    private UUID customerId;
    private String addressType;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private boolean primary;
    private Instant createdAt;
    private Instant updatedAt;
}
