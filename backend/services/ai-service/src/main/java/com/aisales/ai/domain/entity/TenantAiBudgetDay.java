package com.aisales.ai.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tenant_ai_budget_day")
@IdClass(TenantAiBudgetDay.Pk.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantAiBudgetDay {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Id
    @Column(name = "usage_day", nullable = false)
    private LocalDate usageDay;

    @Column(name = "reserved_total", nullable = false)
    private long reservedTotal;

    @Column(name = "reserved_execute", nullable = false)
    private long reservedExecute;

    @Column(name = "reserved_embed", nullable = false)
    private long reservedEmbed;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pk implements Serializable {
        private UUID tenantId;
        private LocalDate usageDay;
    }
}
