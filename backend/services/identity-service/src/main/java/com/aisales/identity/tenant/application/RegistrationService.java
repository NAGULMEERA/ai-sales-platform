package com.aisales.identity.tenant.application;

import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.events.model.TenantCreatedEvent;
import com.aisales.common.events.model.UserCreatedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aisales.identity.audit.application.AuditService;
import com.aisales.identity.audit.domain.AuditAction;
import com.aisales.identity.audit.domain.AuditDetails;
import com.aisales.identity.authentication.application.EmailVerificationService;
import com.aisales.identity.authentication.application.TokenService;
import com.aisales.identity.authentication.infrastructure.configuration.AuthProperties;
import com.aisales.identity.subscription.domain.entity.TenantSubscription;
import com.aisales.identity.subscription.domain.enums.SubscriptionPlan;
import com.aisales.identity.subscription.domain.enums.SubscriptionStatus;
import com.aisales.identity.subscription.infrastructure.persistence.TenantSubscriptionRepository;
import com.aisales.identity.tenant.api.request.RegisterRequest;
import com.aisales.identity.tenant.api.response.RegistrationResponse;
import com.aisales.identity.tenant.domain.entity.Tenant;
import com.aisales.identity.tenant.domain.enums.TenantStatus;
import com.aisales.identity.tenant.infrastructure.persistence.TenantRepository;
import com.aisales.identity.user.domain.entity.User;
import com.aisales.identity.user.infrastructure.persistence.UserRepository;



/**
 * Tenant onboarding orchestration. All persistence for tenant, subscription, and admin user
 * runs in one {@link Transactional} boundary. Role assignment uses the global RBAC catalog
 * seeded by Flyway ({@code TENANT_ADMIN} role permissions). Integration events are written via
 * the Outbox ({@code aisales.events.outbox.enabled=true}) and published after commit.
 */
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final TenantRepository tenantRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailVerificationService emailVerificationService;
    private final AuditService auditService;
    private final EventPublisher eventPublisher;
    private final AuthProperties authProperties;
    private final SlugGenerator slugGenerator;

    @Transactional
    public RegistrationResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already registered");
        }

        String slug = slugGenerator.generate(request.getCompanyName());
        if (tenantRepository.existsBySlug(slug)) {
            slug = slugGenerator.generateUnique(slug);
        }

        UUID organizationId = UUID.randomUUID();
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name(request.getCompanyName())
                .slug(slug)
                .status(TenantStatus.ACTIVE)
                .organizationId(organizationId)
                .build());

        TenantSubscription subscription = subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(tenant.getId())
                .plan(SubscriptionPlan.FREE)
                .status(SubscriptionStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        User user = userRepository.save(User.builder()
                .tenantId(tenant.getId())
                .organizationId(organizationId)
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status(User.UserStatus.ACTIVE)
                .emailVerified(false)
                .roles(Set.of("TENANT_ADMIN"))
                .build());

        eventPublisher.publish(TenantCreatedEvent.of(
                tenant.getId().toString(),
                tenant.getName(),
                tenant.getSlug(),
                SubscriptionPlan.FREE.name(),
                "REAL_ESTATE",
                CorrelationIdUtils.getCorrelationId()));
        eventPublisher.publish(UserCreatedEvent.of(
                tenant.getId().toString(), user.getId().toString(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getRoles(),
                CorrelationIdUtils.getCorrelationId()));

        emailVerificationService.issueVerificationToken(
                tenant.getId(), user.getId(), user.getEmail(), user.getFirstName());
        auditService.logSecurityEvent(tenant.getId(), user.getId(), AuditAction.USER_REGISTERED, "user",
                user.getId().toString(), ipAddress, userAgent, null);
        auditService.logSecurityEvent(tenant.getId(), user.getId(), AuditAction.SUBSCRIPTION_CREATED, "subscription",
                subscription.getId().toString(), ipAddress, userAgent, AuditDetails.plan(SubscriptionPlan.FREE.name()));

        RegistrationResponse.RegistrationResponseBuilder response = RegistrationResponse.builder()
                .tenantId(tenant.getId())
                .companyName(tenant.getName())
                .tenantSlug(tenant.getSlug())
                .userId(user.getId())
                .email(user.getEmail())
                .emailVerificationRequired(true);

        if (authProperties.isAutoLoginAfterRegister()) {
            return response
                    .message("Registration successful. Verify your email to unlock all features.")
                    .authentication(tokenService.issueTokens(user, ipAddress, userAgent))
                    .build();
        }

        return response
                .message("Registration successful. Check your email to verify your account before logging in.")
                .build();
    }
}
