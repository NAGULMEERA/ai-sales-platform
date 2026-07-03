package com.aisales.identity.api.controller;

import com.aisales.common.contracts.auth.AuthResponse;
import com.aisales.common.contracts.auth.LoginRequest;
import com.aisales.common.contracts.auth.RefreshTokenRequest;
import com.aisales.common.contracts.user.CreateUserRequest;
import com.aisales.common.contracts.user.UserDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.security.annotation.AllowPublic;
import com.aisales.identity.api.request.ForgotPasswordRequest;
import com.aisales.identity.api.request.LogoutRequest;
import com.aisales.identity.api.request.RegisterRequest;
import com.aisales.identity.api.response.RegistrationResponse;
import com.aisales.identity.api.request.ResendVerificationEmailRequest;
import com.aisales.identity.api.request.ResetPasswordRequest;
import com.aisales.identity.api.request.VerifyEmailRequest;
import com.aisales.identity.api.response.MessageResponse;
import com.aisales.identity.api.response.SessionResponse;
import com.aisales.identity.application.service.AuthService;
import com.aisales.identity.application.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.aisales.common.security.model.UserPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, tokens, password reset, email verification")
public class AuthController {


    private final AuthService authService;
    private final RegistrationService registrationService;

    @AllowPublic
    @PostMapping("/auth/register")
    @Operation(summary = "Register new tenant and admin user")
    public ApiResponse<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        RegistrationResponse response = registrationService.register(request, http.getRemoteAddr(), http.getHeader("User-Agent"));
        return withCorrelation(ApiResponse.ok("Registration successful", response));
    }

    @AllowPublic
    @PostMapping("/auth/login")
    @Operation(summary = "Login with email and password")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        AuthResponse response = authService.login(request, http.getRemoteAddr(), http.getHeader("User-Agent"));
        return withCorrelation(ApiResponse.ok("Login successful", response));
    }

    @AllowPublic
    @PostMapping("/auth/refresh")
    @Operation(summary = "Refresh access token")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        AuthResponse response = authService.refresh(request, http.getRemoteAddr(), http.getHeader("User-Agent"));
        return withCorrelation(ApiResponse.ok(response));
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "Logout and revoke refresh token")
    public ApiResponse<MessageResponse> logout(@Valid @RequestBody LogoutRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal,
                                               HttpServletRequest http) {
        UUID userId = UUID.fromString(principal.getUserId());
        MessageResponse response = authService.logout(request, userId, http.getRemoteAddr(), http.getHeader("User-Agent"));
        return withCorrelation(ApiResponse.ok(response));
    }

    @AllowPublic
    @PostMapping("/auth/verify-email")
    @Operation(summary = "Verify email address")
    public ApiResponse<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return withCorrelation(ApiResponse.ok(authService.verifyEmail(request)));
    }

    @AllowPublic
    @GetMapping("/auth/verify-email")
    @Operation(summary = "Verify email address via link token query parameter")
    public ApiResponse<MessageResponse> verifyEmailFromLink(@RequestParam("token") String token) {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(token);
        return withCorrelation(ApiResponse.ok(authService.verifyEmail(request)));
    }

    @AllowPublic
    @PostMapping("/auth/resend-verification")
    @Operation(summary = "Resend email verification link")
    public ApiResponse<MessageResponse> resendVerification(@Valid @RequestBody ResendVerificationEmailRequest request) {
        return withCorrelation(ApiResponse.ok(authService.resendVerificationEmail(request)));
    }

    @AllowPublic
    @PostMapping("/auth/forgot-password")
    @Operation(summary = "Request password reset email")
    public ApiResponse<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return withCorrelation(ApiResponse.ok(authService.forgotPassword(request)));
    }

    @AllowPublic
    @PostMapping("/auth/reset-password")
    @Operation(summary = "Reset password with token")
    public ApiResponse<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return withCorrelation(ApiResponse.ok(authService.resetPassword(request)));
    }

    @GetMapping("/auth/sessions")
    @Operation(summary = "List active sessions for current user")
    public ApiResponse<List<SessionResponse>> listSessions(@AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = UUID.fromString(principal.getUserId());
        return withCorrelation(ApiResponse.ok(authService.listSessions(userId, null)));
    }

    @PostMapping("/users")
    @Operation(summary = "Create user within tenant")
    public ApiResponse<UserDto> createUser(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody CreateUserRequest request) {
        UserDto user = authService.createUser(tenantId, request);
        return withCorrelation(ApiResponse.ok("User created", user));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID")
    public ApiResponse<UserDto> getUser(@PathVariable UUID id) {
        return withCorrelation(ApiResponse.ok(authService.getUser(id)));
    }

    private <T> ApiResponse<T> withCorrelation(ApiResponse<T> response) {
        response.setCorrelationId(CorrelationIdUtils.getCorrelationId());
        return response;
    }
}
