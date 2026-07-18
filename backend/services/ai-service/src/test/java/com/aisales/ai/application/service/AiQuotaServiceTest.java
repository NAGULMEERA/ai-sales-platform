package com.aisales.ai.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.aisales.ai.domain.entity.TenantAiQuota;
import com.aisales.ai.infrastructure.configuration.AiQuotaProperties;
import com.aisales.ai.infrastructure.persistence.TenantAiQuotaRepository;
import com.aisales.ai.infrastructure.persistence.TokenUsageRepository;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.model.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiQuotaServiceTest {

    @Mock private TenantAiQuotaRepository tenantAiQuotaRepository;
    @Mock private TokenUsageRepository tokenUsageRepository;

    private AiQuotaProperties properties;
    private AiQuotaService service;
    private UUID tenantId;
    private Instant fixedInstant;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        fixedInstant = LocalDate.of(2026, 7, 18).atStartOfDay(ZoneOffset.UTC).toInstant();
        Clock clock = Clock.fixed(fixedInstant, ZoneOffset.UTC);
        properties = new AiQuotaProperties();
        properties.setEnabled(true);
        properties.setDailyTokenLimit(1000);
        service = new AiQuotaService(properties, tenantAiQuotaRepository, tokenUsageRepository, clock);
    }

    @Test
    void shouldAllowWhenUnderDefaultLimit() {
        when(tenantAiQuotaRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(tokenUsageRepository.sumTotalTokensSince(eq(tenantId), any())).thenReturn(100L);

        assertThatCode(() -> service.assertWithinDailyBudget(tenantId)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectWhenAtOrOverLimit() {
        when(tenantAiQuotaRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(tokenUsageRepository.sumTotalTokensSince(eq(tenantId), any())).thenReturn(1000L);

        assertThatThrownBy(() -> service.assertWithinDailyBudget(tenantId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AI_BUDGET_EXCEEDED);
    }

    @Test
    void shouldUseTenantOverrideLimit() {
        when(tenantAiQuotaRepository.findByTenantId(tenantId))
                .thenReturn(Optional.of(TenantAiQuota.builder()
                        .tenantId(tenantId)
                        .dailyTokenLimit(50)
                        .enabled(true)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build()));
        when(tokenUsageRepository.sumTotalTokensSince(eq(tenantId), any())).thenReturn(50L);

        assertThatThrownBy(() -> service.assertWithinDailyBudget(tenantId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldSkipWhenDisabled() {
        properties.setEnabled(false);
        assertThatCode(() -> service.assertWithinDailyBudget(tenantId)).doesNotThrowAnyException();
    }
}
