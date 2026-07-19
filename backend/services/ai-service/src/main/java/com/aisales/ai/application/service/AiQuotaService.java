package com.aisales.ai.application.service;

import com.aisales.ai.domain.entity.TenantAiBudgetDay;
import com.aisales.ai.domain.entity.TenantAiQuota;
import com.aisales.ai.infrastructure.configuration.AiQuotaProperties;
import com.aisales.ai.infrastructure.persistence.TenantAiBudgetDayRepository;
import com.aisales.ai.infrastructure.persistence.TenantAiQuotaRepository;
import com.aisales.ai.infrastructure.persistence.TokenUsageRepository;
import com.aisales.common.contracts.ai.AiQuotaStatusDto;
import com.aisales.common.contracts.ai.UpsertAiQuotaRequest;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.exception.model.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Soft daily token budgets for LLM EXECUTE and EMBED. Uses {@link ErrorCode#AI_BUDGET_EXCEEDED}
 * ({@code AI_003}) — distinct from gateway RPS {@link ErrorCode#AI_RATE_LIMIT} ({@code AI_001}).
 *
 * <p>Concurrent callers must {@link #reserve} before provider work and {@link #release} afterward
 * so in-flight estimates cannot overspend the ledger.
 */
@Service
@RequiredArgsConstructor
public class AiQuotaService {

    public static final String OPERATION_EXECUTE = "EXECUTE";
    public static final String OPERATION_EMBED = "EMBED";

    private final AiQuotaProperties properties;
    private final TenantAiQuotaRepository tenantAiQuotaRepository;
    private final TenantAiBudgetDayRepository budgetDayRepository;
    private final TokenUsageRepository tokenUsageRepository;
    private final Clock clock;

    /**
     * Overall budget check (legacy). Prefer {@link #assertWithinDailyBudget(UUID, String)}.
     */
    @Transactional(readOnly = true)
    public void assertWithinDailyBudget(UUID tenantId) {
        assertWithinDailyBudget(tenantId, null);
    }

    /**
     * Read-only check of ledger + outstanding reservations (no reservation created).
     *
     * @param operation {@code EXECUTE}, {@code EMBED}, or null for overall-only check
     */
    @Transactional(readOnly = true)
    public void assertWithinDailyBudget(UUID tenantId, String operation) {
        if (!properties.isEnabled()) {
            return;
        }
        ResolvedLimits limits = resolveLimits(tenantId);
        Instant dayStart = dayStart();
        LocalDate usageDay = LocalDate.now(clock);
        TenantAiBudgetDay reserved = budgetDayRepository
                .findById(new TenantAiBudgetDay.Pk(tenantId, usageDay))
                .orElse(null);

        long usedTotal = tokenUsageRepository.sumTotalTokensSince(tenantId, dayStart);
        assertUnder(
                "overall",
                usedTotal + reservedAmount(reserved, null),
                limits.dailyTokenLimit());

        if (OPERATION_EXECUTE.equalsIgnoreCase(operation) && limits.dailyExecuteTokenLimit() > 0) {
            long usedExecute = tokenUsageRepository.sumTotalTokensSinceByOperation(
                    tenantId, OPERATION_EXECUTE, dayStart);
            assertUnder(
                    "EXECUTE",
                    usedExecute + reservedAmount(reserved, OPERATION_EXECUTE),
                    limits.dailyExecuteTokenLimit());
        }
        if (OPERATION_EMBED.equalsIgnoreCase(operation) && limits.dailyEmbedTokenLimit() > 0) {
            long usedEmbed = tokenUsageRepository.sumTotalTokensSinceByOperation(
                    tenantId, OPERATION_EMBED, dayStart);
            assertUnder(
                    "EMBED",
                    usedEmbed + reservedAmount(reserved, OPERATION_EMBED),
                    limits.dailyEmbedTokenLimit());
        }
    }

    @Transactional
    public long reserveExecute(UUID tenantId) {
        return reserve(tenantId, OPERATION_EXECUTE, properties.getReserveExecuteTokens());
    }

    @Transactional
    public long reserveEmbed(UUID tenantId) {
        return reserve(tenantId, OPERATION_EMBED, properties.getReserveEmbedTokens());
    }

    /**
     * Atomically reserves {@code estimate} tokens against today's budget before a provider call.
     *
     * @return the reserved amount (0 when enforcement disabled or estimate &lt;= 0)
     */
    @Transactional
    public long reserve(UUID tenantId, String operation, long estimate) {
        if (!properties.isEnabled() || estimate <= 0) {
            return 0L;
        }
        ResolvedLimits limits = resolveLimits(tenantId);
        Instant dayStart = dayStart();
        LocalDate usageDay = LocalDate.now(clock);
        TenantAiBudgetDay row = lockOrCreateBudgetDay(tenantId, usageDay);

        long usedTotal = tokenUsageRepository.sumTotalTokensSince(tenantId, dayStart);
        assertUnder("overall", usedTotal + row.getReservedTotal() + estimate, limits.dailyTokenLimit());

        if (OPERATION_EXECUTE.equalsIgnoreCase(operation) && limits.dailyExecuteTokenLimit() > 0) {
            long usedExecute = tokenUsageRepository.sumTotalTokensSinceByOperation(
                    tenantId, OPERATION_EXECUTE, dayStart);
            assertUnder(
                    "EXECUTE",
                    usedExecute + row.getReservedExecute() + estimate,
                    limits.dailyExecuteTokenLimit());
            row.setReservedExecute(row.getReservedExecute() + estimate);
        } else if (OPERATION_EMBED.equalsIgnoreCase(operation) && limits.dailyEmbedTokenLimit() > 0) {
            long usedEmbed = tokenUsageRepository.sumTotalTokensSinceByOperation(
                    tenantId, OPERATION_EMBED, dayStart);
            assertUnder(
                    "EMBED",
                    usedEmbed + row.getReservedEmbed() + estimate,
                    limits.dailyEmbedTokenLimit());
            row.setReservedEmbed(row.getReservedEmbed() + estimate);
        }

        row.setReservedTotal(row.getReservedTotal() + estimate);
        row.setUpdatedAt(Instant.now(clock));
        budgetDayRepository.save(row);
        return estimate;
    }

    @Transactional
    public void release(UUID tenantId, String operation, long estimate) {
        if (!properties.isEnabled() || estimate <= 0) {
            return;
        }
        LocalDate usageDay = LocalDate.now(clock);
        budgetDayRepository.findForUpdate(tenantId, usageDay).ifPresent(row -> {
            row.setReservedTotal(Math.max(0, row.getReservedTotal() - estimate));
            if (OPERATION_EXECUTE.equalsIgnoreCase(operation)) {
                row.setReservedExecute(Math.max(0, row.getReservedExecute() - estimate));
            } else if (OPERATION_EMBED.equalsIgnoreCase(operation)) {
                row.setReservedEmbed(Math.max(0, row.getReservedEmbed() - estimate));
            }
            row.setUpdatedAt(Instant.now(clock));
            budgetDayRepository.save(row);
        });
    }

    @Transactional(readOnly = true)
    public AiQuotaStatusDto getStatus(UUID tenantId) {
        ResolvedLimits limits = resolveLimits(tenantId);
        Instant dayStart = dayStart();
        long usedTotal = tokenUsageRepository.sumTotalTokensSince(tenantId, dayStart);
        long usedExecute = tokenUsageRepository.sumTotalTokensSinceByOperation(
                tenantId, OPERATION_EXECUTE, dayStart);
        long usedEmbed = tokenUsageRepository.sumTotalTokensSinceByOperation(
                tenantId, OPERATION_EMBED, dayStart);

        Optional<TenantAiQuota> override = tenantAiQuotaRepository.findByTenantId(tenantId);
        return AiQuotaStatusDto.builder()
                .tenantId(tenantId)
                .quotaEnforcementEnabled(properties.isEnabled())
                .tenantOverride(override.filter(TenantAiQuota::isEnabled).isPresent())
                .planCode(override.map(TenantAiQuota::getPlanCode).orElse(null))
                .limitSource(override.filter(TenantAiQuota::isEnabled).isPresent() ? "TENANT" : "DEFAULT")
                .dailyTokenLimit(limits.dailyTokenLimit())
                .dailyExecuteTokenLimit(limits.dailyExecuteTokenLimit())
                .dailyEmbedTokenLimit(limits.dailyEmbedTokenLimit())
                .usedTotalTokens(usedTotal)
                .usedExecuteTokens(usedExecute)
                .usedEmbedTokens(usedEmbed)
                .remainingTotalTokens(remaining(limits.dailyTokenLimit(), usedTotal))
                .remainingExecuteTokens(remaining(limits.dailyExecuteTokenLimit(), usedExecute))
                .remainingEmbedTokens(remaining(limits.dailyEmbedTokenLimit(), usedEmbed))
                .build();
    }

    @Transactional
    public AiQuotaStatusDto upsert(UUID tenantId, UpsertAiQuotaRequest request) {
        if (request.getDailyTokenLimit() == null || request.getDailyTokenLimit() < 1) {
            throw new ValidationException("dailyTokenLimit must be >= 1");
        }
        Instant now = Instant.now(clock);
        TenantAiQuota quota = tenantAiQuotaRepository.findByTenantId(tenantId)
                .orElseGet(() -> TenantAiQuota.builder()
                        .tenantId(tenantId)
                        .organizationId(parseUuidOrNull(TenantContext.getOrganizationId()))
                        .createdAt(now)
                        .build());

        quota.setDailyTokenLimit(request.getDailyTokenLimit());
        if (request.getDailyExecuteTokenLimit() != null) {
            quota.setDailyExecuteTokenLimit(request.getDailyExecuteTokenLimit());
        } else if (quota.getDailyExecuteTokenLimit() == null) {
            quota.setDailyExecuteTokenLimit(properties.getDailyExecuteTokenLimit());
        }
        if (request.getDailyEmbedTokenLimit() != null) {
            quota.setDailyEmbedTokenLimit(request.getDailyEmbedTokenLimit());
        } else if (quota.getDailyEmbedTokenLimit() == null) {
            quota.setDailyEmbedTokenLimit(properties.getDailyEmbedTokenLimit());
        }
        if (request.getEnabled() != null) {
            quota.setEnabled(request.getEnabled());
        }
        if (StringUtils.hasText(request.getPlanCode())) {
            quota.setPlanCode(request.getPlanCode().trim().toUpperCase(Locale.ROOT));
        } else if (!StringUtils.hasText(quota.getPlanCode())) {
            quota.setPlanCode("CUSTOM");
        }
        quota.setUpdatedAt(now);
        if (quota.getCreatedAt() == null) {
            quota.setCreatedAt(now);
        }
        tenantAiQuotaRepository.save(quota);
        return getStatus(tenantId);
    }

    /**
     * Applies a configured plan package (FREE / PREMIUM). Overwrites tenant override limits.
     */
    @Transactional
    public AiQuotaStatusDto applyPlanPackage(UUID tenantId, String planCode) {
        if (!StringUtils.hasText(planCode)) {
            throw new ValidationException("planCode is required");
        }
        String plan = planCode.trim().toUpperCase(Locale.ROOT);
        AiQuotaProperties.PlanPackage pack = properties.getPlans().get(plan);
        if (pack == null) {
            throw new ValidationException("Unknown AI quota plan package: " + plan);
        }
        return upsert(tenantId, UpsertAiQuotaRequest.builder()
                .dailyTokenLimit(pack.getDailyTokenLimit())
                .dailyExecuteTokenLimit(pack.getDailyExecuteTokenLimit())
                .dailyEmbedTokenLimit(pack.getDailyEmbedTokenLimit())
                .enabled(true)
                .planCode(plan)
                .build());
    }

    private TenantAiBudgetDay lockOrCreateBudgetDay(UUID tenantId, LocalDate usageDay) {
        Optional<TenantAiBudgetDay> existing = budgetDayRepository.findForUpdate(tenantId, usageDay);
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            budgetDayRepository.saveAndFlush(TenantAiBudgetDay.builder()
                    .tenantId(tenantId)
                    .usageDay(usageDay)
                    .reservedTotal(0)
                    .reservedExecute(0)
                    .reservedEmbed(0)
                    .updatedAt(Instant.now(clock))
                    .build());
        } catch (DataIntegrityViolationException ignored) {
            // Concurrent creator won the insert.
        }
        return budgetDayRepository
                .findForUpdate(tenantId, usageDay)
                .orElseThrow(() -> new IllegalStateException("Failed to lock AI budget day row"));
    }

    private static long reservedAmount(TenantAiBudgetDay row, String operation) {
        if (row == null) {
            return 0L;
        }
        if (OPERATION_EXECUTE.equalsIgnoreCase(operation)) {
            return row.getReservedExecute();
        }
        if (OPERATION_EMBED.equalsIgnoreCase(operation)) {
            return row.getReservedEmbed();
        }
        return row.getReservedTotal();
    }

    private void assertUnder(String bucket, long used, long limit) {
        if (limit <= 0) {
            return;
        }
        if (used >= limit) {
            throw new BusinessException(
                    ErrorCode.AI_BUDGET_EXCEEDED,
                    "Daily AI token budget exceeded for " + bucket
                            + " (used=" + used + ", limit=" + limit + ")");
        }
    }

    private ResolvedLimits resolveLimits(UUID tenantId) {
        Optional<TenantAiQuota> override = tenantAiQuotaRepository
                .findByTenantId(tenantId)
                .filter(TenantAiQuota::isEnabled);
        if (override.isPresent()) {
            TenantAiQuota q = override.get();
            return new ResolvedLimits(
                    q.getDailyTokenLimit(),
                    q.getDailyExecuteTokenLimit() != null
                            ? q.getDailyExecuteTokenLimit()
                            : properties.getDailyExecuteTokenLimit(),
                    q.getDailyEmbedTokenLimit() != null
                            ? q.getDailyEmbedTokenLimit()
                            : properties.getDailyEmbedTokenLimit());
        }
        return new ResolvedLimits(
                properties.getDailyTokenLimit(),
                properties.getDailyExecuteTokenLimit(),
                properties.getDailyEmbedTokenLimit());
    }

    private Instant dayStart() {
        return LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static long remaining(long limit, long used) {
        if (limit <= 0) {
            return Long.MAX_VALUE;
        }
        return Math.max(0, limit - used);
    }

    private static UUID parseUuidOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private record ResolvedLimits(long dailyTokenLimit, long dailyExecuteTokenLimit, long dailyEmbedTokenLimit) {
    }
}
