package com.aisales.identity.oauth.application;

import com.aisales.common.contracts.auth.AuthResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.events.model.UserCreatedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aisales.identity.audit.application.AuditService;
import com.aisales.identity.audit.domain.AuditAction;
import com.aisales.identity.audit.domain.AuditDetails;
import com.aisales.identity.authentication.application.TokenService;
import com.aisales.identity.oauth.domain.entity.OAuthAccount;
import com.aisales.identity.oauth.domain.enums.OAuthProvider;
import com.aisales.identity.oauth.infrastructure.persistence.OAuthAccountRepository;
import com.aisales.identity.user.domain.entity.User;
import com.aisales.identity.user.infrastructure.persistence.UserRepository;



@Service
@RequiredArgsConstructor
public class OAuth2LoginService {

    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final EventPublisher eventPublisher;
    private final AuditService auditService;

    @Transactional
    public AuthResponse processOAuthLogin(OAuth2User oauth2User, OAuthProvider provider,
                                          String ipAddress, String userAgent) {
        String providerUserId = oauth2User.getName();
        String email = oauth2User.getAttribute("email");
        String givenName = Optional.ofNullable(oauth2User.getAttribute("given_name")).map(Object::toString).orElse("OAuth");
        String familyName = Optional.ofNullable(oauth2User.getAttribute("family_name")).map(Object::toString).orElse("User");

        OAuthAccount account = oauthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .orElse(null);

        User user;
        if (account != null) {
            user = userRepository.findById(account.getUserId())
                    .orElseThrow(() -> new IllegalStateException("OAuth account linked to missing user"));
        } else {
            user = userRepository.findByEmail(email).orElseGet(() -> createOAuthUser(email, givenName, familyName));
            oauthAccountRepository.save(OAuthAccount.builder()
                    .userId(user.getId())
                    .provider(provider)
                    .providerUserId(providerUserId)
                    .email(email)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build());
        }

        user.setEmailVerified(true);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        auditService.logSecurityEvent(user.getTenantId(), user.getId(), AuditAction.OAUTH_LOGIN, "user",
                user.getId().toString(), ipAddress, userAgent, AuditDetails.reason(provider.name()));
        return tokenService.issueTokens(user, ipAddress, userAgent);
    }

    private User createOAuthUser(String email, String firstName, String lastName) {
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID organizationId = UUID.randomUUID();
        User user = userRepository.save(User.builder()
                .tenantId(tenantId)
                .organizationId(organizationId)
                .email(email)
                .passwordHash(UUID.randomUUID().toString())
                .firstName(firstName)
                .lastName(lastName)
                .status(User.UserStatus.ACTIVE)
                .emailVerified(true)
                .roles(Set.of("USER"))
                .build());
        eventPublisher.publish(UserCreatedEvent.of(
                tenantId.toString(), user.getId().toString(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getRoles(),
                CorrelationIdUtils.getCorrelationId()));
        return user;
    }
}
