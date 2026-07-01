package com.aisales.common.testing.factory;

import com.aisales.common.contracts.tenant.CreateTenantRequest;
import com.aisales.common.contracts.user.CreateUserRequest;

import java.util.Set;
import java.util.UUID;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static CreateTenantRequest createTenantRequest() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return CreateTenantRequest.builder()
                .name("Test Tenant " + suffix)
                .slug("test-" + suffix)
                .plan("STARTER")
                .build();
    }

    public static CreateUserRequest createUserRequest() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return CreateUserRequest.builder()
                .email("user-" + suffix + "@example.com")
                .password("Password123!")
                .firstName("Test")
                .lastName("User")
                .roles(Set.of("USER"))
                .build();
    }
}
