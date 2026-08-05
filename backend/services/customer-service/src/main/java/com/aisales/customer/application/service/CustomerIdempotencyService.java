package com.aisales.customer.application.service;

import com.aisales.common.contracts.customer.CustomerDto;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.customer.domain.entity.IdempotencyKey;
import com.aisales.customer.infrastructure.persistence.IdempotencyKeyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerIdempotencyService {

    static final String CREATE_CUSTOMER_OPERATION = "CREATE_CUSTOMER";
    private static final Duration TTL = Duration.ofHours(24);

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<CustomerDto> beginCreate(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        String normalizedKey = idempotencyKey.trim();
        UUID tenantId = currentTenantId();
        acquireTransactionLock(tenantId, normalizedKey);
        return findCached(tenantId, normalizedKey);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void storeCreateResponse(String idempotencyKey, UUID resourceId, CustomerDto response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        String normalizedKey = idempotencyKey.trim();
        UUID tenantId = currentTenantId();
        String serializedResponse;
        try {
            serializedResponse = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new ValidationException("Failed to store idempotent customer response");
        }
        Instant now = Instant.now();
        int inserted = idempotencyKeyRepository.insertIgnoreConflict(
                UUID.randomUUID(), tenantId, normalizedKey, CREATE_CUSTOMER_OPERATION, resourceId,
                serializedResponse, HttpStatus.CREATED.value(), now, now.plus(TTL));
        if (inserted == 0) {
            // A concurrent request already stored a response for this key (the advisory
            // lock in beginCreate should make this rare). ON CONFLICT DO NOTHING never
            // aborts the surrounding transaction, so this lookup is safe to run here.
            findCached(tenantId, normalizedKey)
                    .orElseThrow(() -> new ValidationException("Idempotency key conflict without stored response"));
        }
    }

    private Optional<CustomerDto> findCached(UUID tenantId, String normalizedKey) {
        return idempotencyKeyRepository.findByTenantIdAndIdempotencyKey(tenantId, normalizedKey)
                .filter(key -> CREATE_CUSTOMER_OPERATION.equals(key.getOperation()))
                .filter(key -> !key.isExpired())
                .map(this::deserialize);
    }

    private UUID currentTenantId() {
        // Idempotency keys are client-supplied and must be scoped per tenant: without this,
        // two tenants submitting the same key value could collide and one tenant would be
        // served the other tenant's cached response (cross-tenant data leak).
        return TenantContext.getTenantIdAsUuid()
                .orElseThrow(() -> new ValidationException("Tenant context is required for idempotent customer creation"));
    }

    private void acquireTransactionLock(UUID tenantId, String idempotencyKey) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:key))")
                .setParameter("key", tenantId + ":" + idempotencyKey)
                .getSingleResult();
    }

    private CustomerDto deserialize(IdempotencyKey key) {
        try {
            return objectMapper.readValue(key.getResponseBody(), CustomerDto.class);
        } catch (JsonProcessingException ex) {
            throw new ValidationException("Stored idempotent response is invalid");
        }
    }
}
