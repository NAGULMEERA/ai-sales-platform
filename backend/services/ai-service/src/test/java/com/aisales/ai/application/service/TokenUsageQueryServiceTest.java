package com.aisales.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.aisales.ai.infrastructure.persistence.TokenUsageRepository;
import com.aisales.common.contracts.ai.AiUsageSummaryDto;
import com.aisales.common.core.util.TenantContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenUsageQueryServiceTest {

    @Mock private TokenUsageRepository tokenUsageRepository;

    private TokenUsageQueryService service;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        service = new TokenUsageQueryService(tokenUsageRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldAggregateBreakdown() {
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-01T00:00:00Z");
        List<Object[]> rows = List.<Object[]>of(new Object[] {
            "EXECUTE", "gemini", "gemini-2.0-flash", 100L, 50L, 150L, new BigDecimal("0.012"), 2L
        });
        when(tokenUsageRepository.aggregateUsageByModel(eq(tenantId), eq(from), eq(to))).thenReturn(rows);

        AiUsageSummaryDto summary = service.summarize(from, to);

        assertThat(summary.getTenantId()).isEqualTo(tenantId);
        assertThat(summary.getTotalTokens()).isEqualTo(150L);
        assertThat(summary.getEstimatedCostUsd()).isEqualByComparingTo("0.012");
        assertThat(summary.getBreakdown()).hasSize(1);
        assertThat(summary.getBreakdown().get(0).getProvider()).isEqualTo("gemini");
    }
}
