package com.aisales.tenant.application.service;

import com.aisales.common.contracts.tenant.TenantDto;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.tenant.domain.entity.IdempotencyKey;
import com.aisales.tenant.infrastructure.persistence.IdempotencyKeyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantIdempotencyService {

    static final String CREATE_TENANT_OPERATION = "CREATE_TENANT";
    private static final Duration TTL = Duration.ofHours(24);

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Acquires a transaction-scoped advisory lock and returns a cached response when present.
     * Must run inside the same transaction as tenant creation to prevent concurrent duplicate creates.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<TenantDto> beginCreate(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        String normalizedKey = idempotencyKey.trim();
        acquireTransactionLock(normalizedKey);
        return findCached(normalizedKey);
    }

    @Transactional(readOnly = true)
    public Optional<TenantDto> findCachedCreateResponse(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return findCached(idempotencyKey.trim());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void storeCreateResponse(String idempotencyKey, UUID resourceId, TenantDto response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        String normalizedKey = idempotencyKey.trim();
        try {
            Instant now = Instant.now();
            idempotencyKeyRepository.save(IdempotencyKey.builder()
                    .idempotencyKey(normalizedKey)
                    .operation(CREATE_TENANT_OPERATION)
                    .resourceId(resourceId)
                    .responseBody(objectMapper.writeValueAsString(response))
                    .httpStatus(HttpStatus.CREATED.value())
                    .createdAt(now)
                    .expiresAt(now.plus(TTL))
                    .build());
        } catch (JsonProcessingException ex) {
            throw new ValidationException("Failed to store idempotent tenant response");
        } catch (DataIntegrityViolationException ex) {
            findCached(normalizedKey)
                    .orElseThrow(() -> new ValidationException("Idempotency key conflict without stored response"));
        }
    }

    private Optional<TenantDto> findCached(String normalizedKey) {
        return idempotencyKeyRepository.findByIdempotencyKey(normalizedKey)
                .filter(key -> CREATE_TENANT_OPERATION.equals(key.getOperation()))
                .filter(key -> !key.isExpired())
                .map(this::deserialize);
    }

    private void acquireTransactionLock(String idempotencyKey) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:key))")
                .setParameter("key", idempotencyKey)
                .getSingleResult();
    }

    private TenantDto deserialize(IdempotencyKey key) {
        try {
            return objectMapper.readValue(key.getResponseBody(), TenantDto.class);
        } catch (JsonProcessingException ex) {
            throw new ValidationException("Stored idempotent response is invalid");
        }
    }
}
