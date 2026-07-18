package com.aisales.ai.application.service;

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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Soft daily token budget for LLM execute. AI Service owns quotas; billing monetization is separate.
 */
@Service
@RequiredArgsConstructor
public class AiQuotaService {

    private final AiQuotaProperties properties;
    private final TenantAiQuotaRepository tenantAiQuotaRepository;
    private final TokenUsageRepository tokenUsageRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public void assertWithinDailyBudget(UUID tenantId) {
        if (!properties.isEnabled()) {
            return;
        }
        long limit = resolveDailyLimit(tenantId);
        if (limit <= 0) {
            return;
        }
        Instant dayStart = LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC).toInstant();
        long used = tokenUsageRepository.sumTotalTokensSince(tenantId, dayStart);
        if (used >= limit) {
            throw new BusinessException(
                    ErrorCode.AI_BUDGET_EXCEEDED,
                    "Daily AI token budget exceeded for tenant (used=" + used + ", limit=" + limit + ")");
        }
    }

    private long resolveDailyLimit(UUID tenantId) {
        return tenantAiQuotaRepository
                .findByTenantId(tenantId)
                .filter(TenantAiQuota::isEnabled)
                .map(TenantAiQuota::getDailyTokenLimit)
                .orElse(properties.getDailyTokenLimit());
    }
}
