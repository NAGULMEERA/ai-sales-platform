package com.aisales.ai.infrastructure.persistence;

import com.aisales.ai.domain.entity.TenantAiBudgetDay;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantAiBudgetDayRepository extends JpaRepository<TenantAiBudgetDay, TenantAiBudgetDay.Pk> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM TenantAiBudgetDay b WHERE b.tenantId = :tenantId AND b.usageDay = :usageDay")
    Optional<TenantAiBudgetDay> findForUpdate(
            @Param("tenantId") UUID tenantId, @Param("usageDay") LocalDate usageDay);
}
