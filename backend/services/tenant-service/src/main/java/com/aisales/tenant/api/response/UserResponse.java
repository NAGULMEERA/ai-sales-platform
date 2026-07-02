package com.aisales.tenant.api.response;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class UserResponse {

    UUID id;
    UUID tenantId;
    String email;
    String firstName;
    String lastName;
    boolean active;
}
