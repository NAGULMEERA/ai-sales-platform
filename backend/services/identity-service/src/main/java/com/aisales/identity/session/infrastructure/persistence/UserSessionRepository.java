package com.aisales.identity.session.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.aisales.identity.session.domain.entity.UserSession;



public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    List<UserSession> findByUserIdAndRevokedAtIsNull(UUID userId);

    List<UserSession> findByRefreshTokenIdAndRevokedAtIsNull(UUID refreshTokenId);
}
