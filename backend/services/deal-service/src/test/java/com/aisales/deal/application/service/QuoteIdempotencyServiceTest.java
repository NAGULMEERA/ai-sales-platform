package com.aisales.deal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.deal.QuoteDto;
import com.aisales.common.contracts.deal.QuoteStatus;
import com.aisales.deal.domain.entity.IdempotencyKey;
import com.aisales.deal.infrastructure.persistence.IdempotencyKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuoteIdempotencyServiceTest {

    @Mock private IdempotencyKeyRepository idempotencyKeyRepository;

    private QuoteIdempotencyService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new QuoteIdempotencyService(idempotencyKeyRepository, objectMapper);
    }

    @Test
    void shouldReturnEmptyForBlankKey() {
        assertThat(service.findCachedCreateResponse(null)).isEmpty();
        assertThat(service.findCachedCreateResponse("  ")).isEmpty();
    }

    @Test
    void shouldReturnCachedCreateResponse() throws Exception {
        QuoteDto dto = QuoteDto.builder()
                .id(UUID.randomUUID())
                .status(QuoteStatus.DRAFT)
                .totalAmount(new BigDecimal("99.0000"))
                .build();
        IdempotencyKey key = IdempotencyKey.builder()
                .idempotencyKey("quote-key-1")
                .operation(QuoteIdempotencyService.CREATE_QUOTE_OPERATION)
                .resourceId(dto.getId())
                .responseBody(objectMapper.writeValueAsString(dto))
                .httpStatus(201)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(idempotencyKeyRepository.findByIdempotencyKey("quote-key-1")).thenReturn(Optional.of(key));

        assertThat(service.findCachedCreateResponse("quote-key-1"))
                .isPresent()
                .get()
                .extracting(QuoteDto::getId, QuoteDto::getStatus)
                .containsExactly(dto.getId(), QuoteStatus.DRAFT);
    }

    @Test
    void shouldIgnoreExpiredCachedResponse() throws Exception {
        QuoteDto dto = QuoteDto.builder().id(UUID.randomUUID()).status(QuoteStatus.DRAFT).build();
        IdempotencyKey key = IdempotencyKey.builder()
                .idempotencyKey("expired")
                .operation(QuoteIdempotencyService.CREATE_QUOTE_OPERATION)
                .resourceId(dto.getId())
                .responseBody(objectMapper.writeValueAsString(dto))
                .httpStatus(201)
                .createdAt(Instant.now().minusSeconds(100_000))
                .expiresAt(Instant.now().minusSeconds(1))
                .build();
        when(idempotencyKeyRepository.findByIdempotencyKey("expired")).thenReturn(Optional.of(key));

        assertThat(service.findCachedCreateResponse("expired")).isEmpty();
    }
}
