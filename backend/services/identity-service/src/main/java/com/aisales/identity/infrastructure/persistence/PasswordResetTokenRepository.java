package com.aisales.identity.infrastructure.persistence;

import com.aisales.identity.domain.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenAndConsumedAtIsNull(String token);
}
