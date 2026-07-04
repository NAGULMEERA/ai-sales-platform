package com.aisales.identity.tenant.api.controller;

import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.security.annotation.AllowPublic;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aisales.identity.tenant.api.request.RegisterRequest;
import com.aisales.identity.tenant.api.response.RegistrationResponse;
import com.aisales.identity.tenant.application.RegistrationService;
import com.aisales.identity.tenant.domain.entity.Tenant;
import com.aisales.identity.user.domain.entity.User;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Registration", description = "Tenant onboarding and admin user registration")
public class RegistrationController {

    private final RegistrationService registrationService;

    @AllowPublic
    @PostMapping("/register")
    @Operation(summary = "Register new tenant and admin user")
    public ApiResponse<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        RegistrationResponse response = registrationService.register(
                request, http.getRemoteAddr(), http.getHeader("User-Agent"));
        return withCorrelation(ApiResponse.ok("Registration successful", response));
    }

    private <T> ApiResponse<T> withCorrelation(ApiResponse<T> response) {
        response.setCorrelationId(CorrelationIdUtils.getCorrelationId());
        return response;
    }
}
