package com.aisales.common.contracts.customer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityMatchCandidateDto {

    private UUID customerId;
    private String fullName;
    private String phone;
    private String email;
    private String whatsapp;
    private String customerNumber;
    private IdentityMatchType matchType;
    private BigDecimal score;
    private List<String> matchedOn;
}
