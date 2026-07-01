package com.aisales.tenant.api.controller;

import com.aisales.common.core.dto.ApiResponse;
import com.aisales.tenant.api.request.UserRequest;
import com.aisales.tenant.api.response.UserResponse;
import com.aisales.tenant.application.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenant-users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable UUID id) {
        return ApiResponse.ok(userService.getById(id));
    }

    @GetMapping("/tenant/{tenantId}")
    public ApiResponse<List<UserResponse>> listByTenant(@PathVariable String tenantId) {
        return ApiResponse.ok(userService.listByTenant(tenantId));
    }
}
