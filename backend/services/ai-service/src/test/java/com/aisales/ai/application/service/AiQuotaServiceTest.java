package com.aisales.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.ai.domain.entity.TenantAiQuota;
import com.aisales.ai.infrastructure.configuration.AiQuotaProperties;
import com.aisales.ai.infrastructure.persistence.TenantAiBudgetDayRepository;
import com.aisales.ai.infrastructure.persistence.TenantAiQuotaRepository;
import com.aisales.ai.infrastructure.persistence.TokenUsageRepository;
import com.aisales.common.contracts.ai.AiQuotaStatusDto;
import com.aisales.common.contracts.ai.UpsertAiQuotaRequest;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiQuotaServiceTest {

    @Mock private TenantAiQuotaRepository tenantAiQuotaRepository;
    @Mock private TenantAiBudgetDayRepository budgetDayRepository;
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
        properties.setDailyExecuteTokenLimit(800);
        properties.setDailyEmbedTokenLimit(200);
        service = new AiQuotaService(
                properties, tenantAiQuotaRepository, budgetDayRepository, tokenUsageRepository, clock);
        lenient().when(budgetDayRepository.findById(any())).thenReturn(Optional.empty());
    }

    @Test
    void shouldAllowWhenUnderDefaultLimit() {
        when(tenantAiQuotaRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(tokenUsageRepository.sumTotalTokensSince(eq(tenantId), any())).thenReturn(100L);

        assertThatCode(() -> service.assertWithinDailyBudget(tenantId, AiQuotaService.OPERATION_EXECUTE))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectWhenAtOrOverOverallLimit() {
        when(tenantAiQuotaRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(tokenUsageRepository.sumTotalTokensSince(eq(tenantId), any())).thenReturn(1000L);

        assertThatThrownBy(() -> service.assertWithinDailyBudget(tenantId, AiQuotaService.OPERATION_EXECUTE))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AI_BUDGET_EXCEEDED);
    }

    @Test
    void shouldRejectWhenExecuteBucketExceeded() {
        when(tenantAiQuotaRepository.findByTenantId(tenantId)).thenReturn(Optional.empty());
        when(tokenUsageRepository.sumTotalTokensSince(eq(tenantId), any())).thenReturn(100L);
        when(tokenUsageRepository.sumTotalTokensSinceByOperation(
                        eq(tenantId), eq(AiQuotaService.OPERATION_EXECUTE), any()))
                .thenReturn(800L);

        assertThatThrownBy(() -> service.assertWithinDailyBudget(tenantId, AiQuotaService.OPERATION_EXECUTE))
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
                        .dailyExecuteTokenLimit(40L)
                        .dailyEmbedTokenLimit(10L)
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

    @Test
    void shouldUpsertAndReportStatus() {
        java.util.concurrent.atomic.AtomicReference<TenantAiQuota> stored =
                new java.util.concurrent.atomic.AtomicReference<>();
        when(tenantAiQuotaRepository.findByTenantId(tenantId))
                .thenAnswer(inv -> Optional.ofNullable(stored.get()));
        when(tenantAiQuotaRepository.save(any(TenantAiQuota.class))).thenAnswer(inv -> {
            TenantAiQuota quota = inv.getArgument(0);
            stored.set(quota);
            return quota;
        });
        when(tokenUsageRepository.sumTotalTokensSince(eq(tenantId), any())).thenReturn(10L);
        when(tokenUsageRepository.sumTotalTokensSinceByOperation(eq(tenantId), any(), any())).thenReturn(5L);

        AiQuotaStatusDto status = service.upsert(tenantId, UpsertAiQuotaRequest.builder()
                .dailyTokenLimit(5000L)
                .dailyExecuteTokenLimit(4000L)
                .dailyEmbedTokenLimit(1000L)
                .enabled(true)
                .planCode("CUSTOM")
                .build());

        ArgumentCaptor<TenantAiQuota> captor = ArgumentCaptor.forClass(TenantAiQuota.class);
        verify(tenantAiQuotaRepository).save(captor.capture());
        assertThat(captor.getValue().getDailyTokenLimit()).isEqualTo(5000L);
        assertThat(status.getDailyTokenLimit()).isEqualTo(5000L);
        assertThat(status.getUsedTotalTokens()).isEqualTo(10L);
        assertThat(status.getLimitSource()).isEqualTo("TENANT");
    }

    @Test
    void shouldApplyPremiumPlanPackage() {
        java.util.concurrent.atomic.AtomicReference<TenantAiQuota> stored =
                new java.util.concurrent.atomic.AtomicReference<>();
        when(tenantAiQuotaRepository.findByTenantId(tenantId))
                .thenAnswer(inv -> Optional.ofNullable(stored.get()));
        when(tenantAiQuotaRepository.save(any(TenantAiQuota.class))).thenAnswer(inv -> {
            TenantAiQuota quota = inv.getArgument(0);
            stored.set(quota);
            return quota;
        });
        when(tokenUsageRepository.sumTotalTokensSince(eq(tenantId), any())).thenReturn(0L);
        when(tokenUsageRepository.sumTotalTokensSinceByOperation(eq(tenantId), any(), any())).thenReturn(0L);

        AiQuotaStatusDto status = service.applyPlanPackage(tenantId, "PREMIUM");

        assertThat(status.getPlanCode()).isEqualTo("PREMIUM");
        assertThat(status.getDailyTokenLimit()).isEqualTo(2_000_000L);
    }
}
