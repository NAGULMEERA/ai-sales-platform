package com.aisales.identity.subscription.api.controller;

import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.security.model.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.aisales.identity.subscription.api.response.FeatureCheckResponse;
import com.aisales.identity.subscription.api.response.SubscriptionResponse;
import com.aisales.identity.subscription.application.FeatureCheckService;
import com.aisales.identity.subscription.application.SubscriptionService;
import com.aisales.identity.tenant.domain.entity.Tenant;



@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Subscription & Features", description = "Tenant subscription and feature gating")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final FeatureCheckService featureCheckService;

    @GetMapping("/subscriptions/current")
    @Operation(summary = "Get current tenant subscription")
    public ApiResponse<SubscriptionResponse> getCurrentSubscription() {
        UUID tenantId = UUID.fromString(TenantContext.getTenantId());
        SubscriptionResponse response = subscriptionService.getSubscription(tenantId);
        ApiResponse<SubscriptionResponse> api = ApiResponse.ok(response);
        api.setCorrelationId(CorrelationIdUtils.getCorrelationId());
        return api;
    }

    @PostMapping("/subscriptions/upgrade")
    @Operation(summary = "Upgrade tenant to Premium (payment hook)")
    public ApiResponse<SubscriptionResponse> upgrade(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "externalSubscriptionId", required = false) String externalSubscriptionId) {
        UUID tenantId = UUID.fromString(TenantContext.getTenantId());
        UUID userId = UUID.fromString(principal.getUserId());
        SubscriptionResponse response = subscriptionService.upgradeToPremium(
                tenantId, userId, externalSubscriptionId != null ? externalSubscriptionId : "manual-upgrade");
        ApiResponse<SubscriptionResponse> api = ApiResponse.ok("Upgraded to Premium", response);
        api.setCorrelationId(CorrelationIdUtils.getCorrelationId());
        return api;
    }

    @GetMapping("/features/{featureCode}")
    @Operation(summary = "Check if feature is enabled for tenant plan")
    public ApiResponse<FeatureCheckResponse> checkFeature(@PathVariable("featureCode") String featureCode) {
        UUID tenantId = UUID.fromString(TenantContext.getTenantId());
        FeatureCheckResponse response = featureCheckService.checkFeature(tenantId, featureCode);
        ApiResponse<FeatureCheckResponse> api = ApiResponse.ok(response);
        api.setCorrelationId(CorrelationIdUtils.getCorrelationId());
        return api;
    }
}
