package com.aisales.identity.infrastructure.persistence;

import com.aisales.identity.domain.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    List<UserSession> findByUserIdAndRevokedAtIsNull(UUID userId);

    List<UserSession> findByRefreshTokenIdAndRevokedAtIsNull(UUID refreshTokenId);
}
