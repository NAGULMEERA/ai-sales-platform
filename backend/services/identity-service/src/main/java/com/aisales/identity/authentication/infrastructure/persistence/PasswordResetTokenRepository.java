package com.aisales.identity.authentication.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.aisales.identity.authentication.domain.entity.PasswordResetToken;



public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenAndConsumedAtIsNull(String token);

    long countByUserIdAndCreatedAtAfter(UUID userId, Instant createdAfter);

    Optional<PasswordResetToken> findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("""
            UPDATE PasswordResetToken t
            SET t.consumedAt = :invalidatedAt
            WHERE t.userId = :userId AND t.consumedAt IS NULL
            """)
    int invalidatePendingForUser(@Param("userId") UUID userId, @Param("invalidatedAt") Instant invalidatedAt);

    /** Single-winner consume; returns 1 only for the first concurrent consumer. */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE PasswordResetToken t
            SET t.consumedAt = :consumedAt
            WHERE t.id = :id AND t.consumedAt IS NULL
            """)
    int consumeById(@Param("id") UUID id, @Param("consumedAt") Instant consumedAt);
}
