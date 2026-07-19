package com.aisales.identity.authentication.application;

import com.aisales.common.security.model.TokenInfo;
import com.aisales.common.security.util.JwtTokenProvider;
import com.aisales.identity.subscription.domain.enums.SubscriptionPlan;
import com.aisales.identity.subscription.infrastructure.persistence.TenantSubscriptionRepository;
import com.aisales.identity.user.domain.entity.User;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtTokenProvider jwtTokenProvider;
    private final TenantSubscriptionRepository subscriptionRepository;

    public TokenInfo generateTokens(User user, Set<String> permissions) {
        String organizationId = user.getOrganizationId() != null ? user.getOrganizationId().toString() : null;
        return jwtTokenProvider.generateTokens(
                user.getId().toString(),
                user.getTenantId().toString(),
                organizationId,
                user.getEmail(),
                user.getRoles(),
                permissions,
                resolvePlan(user));
    }

    public TokenInfo generateAccessToken(User user, Set<String> permissions) {
        String organizationId = user.getOrganizationId() != null ? user.getOrganizationId().toString() : null;
        return jwtTokenProvider.generateAccessToken(
                user.getId().toString(),
                user.getTenantId().toString(),
                organizationId,
                user.getEmail(),
                user.getRoles(),
                permissions,
                resolvePlan(user));
    }

    private String resolvePlan(User user) {
        return subscriptionRepository.findByTenantId(user.getTenantId())
                .map(subscription -> subscription.getPlan() != null
                        ? subscription.getPlan().name()
                        : SubscriptionPlan.FREE.name())
                .orElse(SubscriptionPlan.FREE.name());
    }
}
