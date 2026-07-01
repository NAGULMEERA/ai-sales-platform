package com.aisales.common.contracts.client;

import com.aisales.common.contracts.auth.AuthResponse;
import com.aisales.common.contracts.auth.LoginRequest;
import com.aisales.common.contracts.auth.RefreshTokenRequest;
import com.aisales.common.contracts.user.CreateUserRequest;
import com.aisales.common.contracts.user.UserDto;
import com.aisales.common.core.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "identity-service", path = "/api/v1")
public interface IdentityServiceClient {

    @PostMapping("/auth/login")
    ApiResponse<AuthResponse> login(@RequestBody LoginRequest request);

    @PostMapping("/auth/refresh")
    ApiResponse<AuthResponse> refresh(@RequestBody RefreshTokenRequest request);

    @PostMapping("/users")
    ApiResponse<UserDto> createUser(@RequestBody CreateUserRequest request);

    @GetMapping("/users/{id}")
    ApiResponse<UserDto> getUser(@PathVariable UUID id);
}
