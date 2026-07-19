package com.aisales.ai.application.service;

import com.aisales.ai.infrastructure.persistence.TokenUsageRepository;
import com.aisales.common.contracts.ai.AiUsageBreakdownDto;
import com.aisales.common.contracts.ai.AiUsageSummaryDto;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.ValidationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Read API over the AI usage ledger for Billing and ops. AI remains system of record.
 */
@Service
@RequiredArgsConstructor
public class TokenUsageQueryService {

    private final TokenUsageRepository tokenUsageRepository;

    @Transactional(readOnly = true)
    public AiUsageSummaryDto summarize(Instant from, Instant to) {
        UUID tenantId = requireTenantId();
        if (from == null || to == null) {
            throw new ValidationException("from and to are required");
        }
        if (!to.isAfter(from)) {
            throw new ValidationException("to must be after from");
        }

        List<Object[]> rows = tokenUsageRepository.aggregateUsageByModel(tenantId, from, to);
        List<AiUsageBreakdownDto> breakdown = new ArrayList<>();
        long promptTokens = 0;
        long completionTokens = 0;
        long totalTokens = 0;
        long requestCount = 0;
        BigDecimal estimatedCost = BigDecimal.ZERO;

        for (Object[] row : rows) {
            long rowPrompt = ((Number) row[3]).longValue();
            long rowCompletion = ((Number) row[4]).longValue();
            long rowTotal = ((Number) row[5]).longValue();
            BigDecimal rowCost = row[6] instanceof BigDecimal bd ? bd : new BigDecimal(row[6].toString());
            long rowCount = ((Number) row[7]).longValue();

            promptTokens += rowPrompt;
            completionTokens += rowCompletion;
            totalTokens += rowTotal;
            requestCount += rowCount;
            estimatedCost = estimatedCost.add(rowCost);

            breakdown.add(AiUsageBreakdownDto.builder()
                    .operation(String.valueOf(row[0]))
                    .provider(String.valueOf(row[1]))
                    .model(String.valueOf(row[2]))
                    .promptTokens(rowPrompt)
                    .completionTokens(rowCompletion)
                    .totalTokens(rowTotal)
                    .estimatedCostUsd(rowCost)
                    .requestCount(rowCount)
                    .build());
        }

        return AiUsageSummaryDto.builder()
                .tenantId(tenantId)
                .periodFrom(from)
                .periodTo(to)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .estimatedCostUsd(estimatedCost)
                .requestCount(requestCount)
                .breakdown(breakdown)
                .build();
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }
}
