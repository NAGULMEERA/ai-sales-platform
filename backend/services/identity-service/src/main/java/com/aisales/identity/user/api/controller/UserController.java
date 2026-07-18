package com.aisales.identity.user.api.controller;

import com.aisales.common.contracts.user.CreateUserRequest;
import com.aisales.common.contracts.user.UserDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aisales.identity.tenant.domain.entity.Tenant;
import com.aisales.identity.user.application.UserService;
import com.aisales.identity.user.domain.entity.User;



@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Tenant-scoped user management")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create user within tenant")
    public ApiResponse<UserDto> createUser(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody CreateUserRequest request) {
        UserDto user = userService.createUser(tenantId, request);
        return withCorrelation(ApiResponse.ok("User created", user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ApiResponse<UserDto> getUser(@PathVariable("id") UUID id) {
        return withCorrelation(ApiResponse.ok(userService.getUser(id)));
    }

    private <T> ApiResponse<T> withCorrelation(ApiResponse<T> response) {
        response.setCorrelationId(CorrelationIdUtils.getCorrelationId());
        return response;
    }
}
