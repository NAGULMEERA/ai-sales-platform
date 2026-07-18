package com.aisales.deal.application.service;

import com.aisales.common.contracts.deal.QuoteDto;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.deal.domain.entity.IdempotencyKey;
import com.aisales.deal.infrastructure.persistence.IdempotencyKeyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuoteIdempotencyService {

    static final String CREATE_QUOTE_OPERATION = "CREATE_QUOTE";
    private static final Duration TTL = Duration.ofHours(24);

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Optional<QuoteDto> findCachedCreateResponse(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return findCached(idempotencyKey.trim());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<QuoteDto> beginCreate(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        String normalizedKey = idempotencyKey.trim();
        acquireTransactionLock(normalizedKey);
        return findCached(normalizedKey);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void storeCreateResponse(String idempotencyKey, UUID resourceId, QuoteDto response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        String normalizedKey = idempotencyKey.trim();
        try {
            Instant now = Instant.now();
            idempotencyKeyRepository.save(IdempotencyKey.builder()
                    .idempotencyKey(normalizedKey)
                    .operation(CREATE_QUOTE_OPERATION)
                    .resourceId(resourceId)
                    .responseBody(objectMapper.writeValueAsString(response))
                    .httpStatus(HttpStatus.CREATED.value())
                    .createdAt(now)
                    .expiresAt(now.plus(TTL))
                    .build());
        } catch (JsonProcessingException ex) {
            throw new ValidationException("Failed to store idempotent quote response");
        } catch (DataIntegrityViolationException ex) {
            findCached(normalizedKey)
                    .orElseThrow(() -> new ValidationException("Idempotency key conflict without stored response"));
        }
    }

    private Optional<QuoteDto> findCached(String normalizedKey) {
        return idempotencyKeyRepository.findByIdempotencyKey(normalizedKey)
                .filter(key -> CREATE_QUOTE_OPERATION.equals(key.getOperation()))
                .filter(key -> !key.isExpired())
                .map(this::deserialize);
    }

    private void acquireTransactionLock(String idempotencyKey) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:key))")
                .setParameter("key", idempotencyKey)
                .getSingleResult();
    }

    private QuoteDto deserialize(IdempotencyKey key) {
        try {
            return objectMapper.readValue(key.getResponseBody(), QuoteDto.class);
        } catch (JsonProcessingException ex) {
            throw new ValidationException("Stored idempotent response is invalid");
        }
    }
}
