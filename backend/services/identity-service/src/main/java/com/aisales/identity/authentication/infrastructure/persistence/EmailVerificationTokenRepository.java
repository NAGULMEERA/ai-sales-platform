package com.aisales.identity.authentication.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.aisales.identity.authentication.domain.entity.EmailVerificationToken;



public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenAndConsumedAtIsNull(String token);

    long countByUserIdAndCreatedAtAfter(UUID userId, Instant createdAfter);

    Optional<EmailVerificationToken> findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("""
            UPDATE EmailVerificationToken t
            SET t.consumedAt = :invalidatedAt
            WHERE t.userId = :userId AND t.consumedAt IS NULL
            """)
    int invalidatePendingForUser(@Param("userId") UUID userId, @Param("invalidatedAt") Instant invalidatedAt);

    /** Single-winner consume; returns 1 only for the first concurrent consumer. */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE EmailVerificationToken t
            SET t.consumedAt = :consumedAt
            WHERE t.id = :id AND t.consumedAt IS NULL
            """)
    int consumeById(@Param("id") UUID id, @Param("consumedAt") Instant consumedAt);
}
