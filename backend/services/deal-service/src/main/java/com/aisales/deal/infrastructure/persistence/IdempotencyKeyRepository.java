package com.aisales.deal.infrastructure.persistence;

import com.aisales.deal.domain.entity.IdempotencyKey;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByIdempotencyKey(String idempotencyKey);

    /**
     * Atomic insert. Returns 1 when this call stored the response, 0 when a concurrent
     * request already claimed the key. Does not abort the surrounding PostgreSQL
     * transaction on conflict (unlike a plain save() + caught constraint violation).
     */
    @Modifying
    @Query(value = """
            INSERT INTO idempotency_keys
                (id, idempotency_key, operation, resource_id, response_body, http_status, created_at, expires_at)
            VALUES (:id, :idempotencyKey, :operation, :resourceId, :responseBody, :httpStatus, :createdAt, :expiresAt)
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIgnoreConflict(@Param("id") UUID id,
                              @Param("idempotencyKey") String idempotencyKey,
                              @Param("operation") String operation,
                              @Param("resourceId") UUID resourceId,
                              @Param("responseBody") String responseBody,
                              @Param("httpStatus") int httpStatus,
                              @Param("createdAt") Instant createdAt,
                              @Param("expiresAt") Instant expiresAt);
}
