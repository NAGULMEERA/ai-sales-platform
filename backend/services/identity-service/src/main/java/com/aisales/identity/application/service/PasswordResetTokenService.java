package com.aisales.identity.application.service;

import com.aisales.identity.application.event.PasswordResetRequestedEvent;
import com.aisales.identity.domain.entity.PasswordResetToken;
import com.aisales.identity.infrastructure.configuration.AuthProperties;
import com.aisales.identity.infrastructure.persistence.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthProperties authProperties;

    @Transactional
    public void issueResetToken(UUID tenantId, UUID userId, String email, String firstName) {
        String token = UUID.randomUUID().toString();
        tokenRepository.save(PasswordResetToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(Instant.now().plusSeconds(authProperties.getPasswordResetExpirationHours() * 3600))
                .createdAt(Instant.now())
                .build());
        // Published now, delivered only after this transaction commits (see EmailEventListener).
        eventPublisher.publishEvent(new PasswordResetRequestedEvent(tenantId, email, firstName, token));
    }
}
