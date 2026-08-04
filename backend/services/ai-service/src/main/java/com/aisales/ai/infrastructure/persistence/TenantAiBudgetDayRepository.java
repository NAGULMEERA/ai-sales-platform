package com.aisales.ai.infrastructure.persistence;

import com.aisales.ai.domain.entity.TenantAiBudgetDay;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantAiBudgetDayRepository extends JpaRepository<TenantAiBudgetDay, TenantAiBudgetDay.Pk> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM TenantAiBudgetDay b WHERE b.tenantId = :tenantId AND b.usageDay = :usageDay")
    Optional<TenantAiBudgetDay> findForUpdate(
            @Param("tenantId") UUID tenantId, @Param("usageDay") LocalDate usageDay);

    /**
     * Atomic insert of an empty budget row. Returns 1 when this call created the row,
     * 0 when a concurrent caller already won the insert. Does not abort the surrounding
     * PostgreSQL transaction on conflict (unlike a plain saveAndFlush() + caught
     * constraint violation).
     */
    @Modifying
    @Query(value = """
            INSERT INTO tenant_ai_budget_day (tenant_id, usage_day, reserved_total, reserved_execute, reserved_embed, updated_at)
            VALUES (:tenantId, :usageDay, 0, 0, 0, :updatedAt)
            ON CONFLICT (tenant_id, usage_day) DO NOTHING
            """, nativeQuery = true)
    int insertIgnoreConflict(@Param("tenantId") UUID tenantId,
                              @Param("usageDay") LocalDate usageDay,
                              @Param("updatedAt") Instant updatedAt);
}
