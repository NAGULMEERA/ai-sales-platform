package com.aisales.lead.infrastructure.persistence;

import com.aisales.lead.domain.entity.IdempotencyKey;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    /**
     * Tenant-scoped lookup. Idempotency keys are client-supplied strings and must never be
     * resolved across tenants: two tenants can legitimately submit the same key value, and
     * without the tenant_id predicate one tenant would be served another tenant's cached
     * response (cross-tenant data leak).
     */
    Optional<IdempotencyKey> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    /**
     * Atomic insert. Returns 1 when this call stored the response, 0 when a concurrent
     * request already claimed the (tenant, key) pair. Does not abort the surrounding
     * PostgreSQL transaction on conflict (unlike a plain save() + caught constraint violation).
     */
    @Modifying
    @Query(value = """
            INSERT INTO idempotency_keys
                (id, tenant_id, idempotency_key, operation, resource_id, response_body, http_status, created_at, expires_at)
            VALUES (:id, :tenantId, :idempotencyKey, :operation, :resourceId, :responseBody, :httpStatus, :createdAt, :expiresAt)
            ON CONFLICT (tenant_id, idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIgnoreConflict(@Param("id") UUID id,
                              @Param("tenantId") UUID tenantId,
                              @Param("idempotencyKey") String idempotencyKey,
                              @Param("operation") String operation,
                              @Param("resourceId") UUID resourceId,
                              @Param("responseBody") String responseBody,
                              @Param("httpStatus") int httpStatus,
                              @Param("createdAt") Instant createdAt,
                              @Param("expiresAt") Instant expiresAt);
}
