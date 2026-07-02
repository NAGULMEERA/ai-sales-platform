package com.aisales.tenant.application.service;

import com.aisales.common.contracts.tenant.TenantDto;
import com.aisales.common.contracts.tenant.TenantStatus;
import com.aisales.tenant.domain.entity.IdempotencyKey;
import com.aisales.tenant.infrastructure.persistence.IdempotencyKeyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantIdempotencyServiceTest {

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private Query lockQuery;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TenantIdempotencyService tenantIdempotencyService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tenantIdempotencyService, "entityManager", entityManager);
    }

    @Test
    void shouldReturnCachedCreateResponseWhenKeyExists() throws Exception {
        UUID tenantId = UUID.randomUUID();
        TenantDto tenantDto = TenantDto.builder()
                .id(tenantId)
                .name("Acme")
                .status(TenantStatus.ACTIVE)
                .build();
        IdempotencyKey key = IdempotencyKey.builder()
                .idempotencyKey("key-1")
                .operation(TenantIdempotencyService.CREATE_TENANT_OPERATION)
                .responseBody(objectMapper.writeValueAsString(tenantDto))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(idempotencyKeyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(key));

        Optional<TenantDto> cached = tenantIdempotencyService.findCachedCreateResponse("key-1");

        assertThat(cached).isPresent();
        assertThat(cached.get().getId()).isEqualTo(tenantId);
    }

    @Test
    void shouldAcquireLockBeforeCreateLookup() throws Exception {
        UUID tenantId = UUID.randomUUID();
        TenantDto tenantDto = TenantDto.builder()
                .id(tenantId)
                .name("Acme")
                .status(TenantStatus.ACTIVE)
                .build();
        String responseJson = new ObjectMapper().writeValueAsString(tenantDto);
        when(entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:key))")).thenReturn(lockQuery);
        when(lockQuery.setParameter("key", "key-1")).thenReturn(lockQuery);
        when(lockQuery.getSingleResult()).thenReturn(1L);
        when(idempotencyKeyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(
                IdempotencyKey.builder()
                        .idempotencyKey("key-1")
                        .operation(TenantIdempotencyService.CREATE_TENANT_OPERATION)
                        .responseBody(responseJson)
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .build()));

        Optional<TenantDto> cached = tenantIdempotencyService.beginCreate("key-1");

        assertThat(cached).isPresent();
        verify(lockQuery).getSingleResult();
    }

    @Test
    void shouldStoreCreateResponse() {
        UUID tenantId = UUID.randomUUID();
        TenantDto tenantDto = TenantDto.builder()
                .id(tenantId)
                .name("Acme")
                .status(TenantStatus.ACTIVE)
                .build();

        tenantIdempotencyService.storeCreateResponse("key-1", tenantId, tenantDto);

        ArgumentCaptor<IdempotencyKey> captor = ArgumentCaptor.forClass(IdempotencyKey.class);
        verify(idempotencyKeyRepository).save(captor.capture());
        IdempotencyKey saved = captor.getValue();
        assertThat(saved.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(saved.getOperation()).isEqualTo(TenantIdempotencyService.CREATE_TENANT_OPERATION);
        assertThat(saved.getResourceId()).isEqualTo(tenantId);
        assertThat(saved.getHttpStatus()).isEqualTo(HttpStatus.CREATED.value());
    }

    @Test
    void shouldIgnoreDuplicateKeyWhenResponseAlreadyStored() throws Exception {
        UUID tenantId = UUID.randomUUID();
        TenantDto tenantDto = TenantDto.builder()
                .id(tenantId)
                .name("Acme")
                .status(TenantStatus.ACTIVE)
                .build();
        String responseJson = new ObjectMapper().writeValueAsString(tenantDto);
        when(idempotencyKeyRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(idempotencyKeyRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(
                IdempotencyKey.builder()
                        .idempotencyKey("key-1")
                        .operation(TenantIdempotencyService.CREATE_TENANT_OPERATION)
                        .responseBody(responseJson)
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .build()));

        tenantIdempotencyService.storeCreateResponse("key-1", tenantId, tenantDto);

        verify(idempotencyKeyRepository).findByIdempotencyKey("key-1");
    }

    @Test
    void shouldIgnoreBlankKey() {
        assertThat(tenantIdempotencyService.findCachedCreateResponse("  ")).isEmpty();
        tenantIdempotencyService.storeCreateResponse(" ", tenantId(), tenantDto());
        verify(idempotencyKeyRepository, never()).save(any());
    }

    private UUID tenantId() {
        return UUID.randomUUID();
    }

    private TenantDto tenantDto() {
        return TenantDto.builder()
                .id(tenantId())
                .name("Acme")
                .status(TenantStatus.ACTIVE)
                .build();
    }
}
