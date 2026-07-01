package com.aisales.identity.infrastructure.persistence;

import com.aisales.identity.domain.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenAndConsumedAtIsNull(String token);
}
