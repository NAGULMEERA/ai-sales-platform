package com.aisales.common.contracts.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private UUID id;
    private UUID tenantId;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    private String status;
    private Instant createdAt;
}
