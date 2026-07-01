package com.aisales.identity.application.service;

import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.events.model.TenantCreatedEvent;
import com.aisales.common.events.model.UserCreatedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.identity.api.request.RegisterRequest;
import com.aisales.identity.api.response.RegistrationResponse;
import com.aisales.identity.application.utils.SlugGenerator;
import com.aisales.identity.domain.entity.Tenant;
import com.aisales.identity.domain.entity.TenantSubscription;
import com.aisales.identity.domain.entity.User;
import com.aisales.identity.domain.enums.SubscriptionPlan;
import com.aisales.identity.domain.enums.SubscriptionStatus;
import com.aisales.identity.domain.enums.TenantStatus;
import com.aisales.identity.infrastructure.configuration.AuthProperties;
import com.aisales.identity.infrastructure.persistence.TenantRepository;
import com.aisales.identity.infrastructure.persistence.TenantSubscriptionRepository;
import com.aisales.identity.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

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

    @Transactional
    public RegistrationResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already registered");
        }

        String slug = SlugGenerator.generate(request.getCompanyName());
        if (tenantRepository.existsBySlug(slug)) {
            slug = SlugGenerator.generateUnique(slug);
        }

        UUID organizationId = UUID.randomUUID();
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name(request.getCompanyName())
                .slug(slug)
                .status(TenantStatus.ACTIVE)
                .organizationId(organizationId)
                .build());

        subscriptionRepository.save(TenantSubscription.builder()
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
                tenant.getId().toString(), tenant.getName(), SubscriptionPlan.FREE.name(),
                CorrelationIdUtils.getCorrelationId()));
        eventPublisher.publish(UserCreatedEvent.of(
                tenant.getId().toString(), user.getId().toString(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getRoles(),
                CorrelationIdUtils.getCorrelationId()));

        emailVerificationService.issueVerificationToken(
                tenant.getId(), user.getId(), user.getEmail(), user.getFirstName());
        auditService.log(tenant.getId(), user.getId(), "USER_REGISTERED", "user", user.getId().toString(),
                ipAddress, userAgent, null);

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
