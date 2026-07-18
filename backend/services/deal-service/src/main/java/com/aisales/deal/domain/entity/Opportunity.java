package com.aisales.deal.domain.entity;

import com.aisales.common.contracts.deal.OpportunityStatus;
import com.aisales.common.exception.exception.ValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "opportunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Opportunity {

    private static final Set<OpportunityStatus> TERMINAL =
            EnumSet.of(OpportunityStatus.WON, OpportunityStatus.LOST, OpportunityStatus.CANCELLED);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private String name;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OpportunityStatus status = OpportunityStatus.OPEN;

    private Integer probability;

    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public void assertActive() {
        if (deletedAt != null) {
            throw new ValidationException("Opportunity is deleted");
        }
    }

    public void updateDetails(String nameValue, BigDecimal amountValue, String currencyValue,
                              Integer probabilityValue, LocalDate expectedCloseDateValue,
                              OpportunityStatus statusValue, UUID actor) {
        assertActive();
        if (isTerminal() && statusValue != null && statusValue != status) {
            throw new ValidationException("Terminal opportunity cannot change status: " + status);
        }
        if (nameValue != null && !nameValue.isBlank()) {
            this.name = nameValue.trim();
        }
        if (amountValue != null) {
            if (amountValue.signum() < 0) {
                throw new ValidationException("Amount cannot be negative");
            }
            this.amount = amountValue;
        }
        if (currencyValue != null && !currencyValue.isBlank()) {
            this.currency = currencyValue.trim().toUpperCase();
        }
        if (probabilityValue != null) {
            if (probabilityValue < 0 || probabilityValue > 100) {
                throw new ValidationException("Probability must be between 0 and 100");
            }
            this.probability = probabilityValue;
        }
        if (expectedCloseDateValue != null) {
            this.expectedCloseDate = expectedCloseDateValue;
        }
        if (statusValue != null) {
            transitionTo(statusValue, actor);
            return;
        }
        touch(actor);
    }

    public void assign(UUID assignee, UUID actor) {
        assertActive();
        if (assignee == null) {
            throw new ValidationException("assignedTo is required");
        }
        if (isTerminal()) {
            throw new ValidationException("Cannot assign a terminal opportunity");
        }
        this.assignedTo = assignee;
        touch(actor);
    }

    public void markQuoted(UUID actor) {
        if (status == OpportunityStatus.OPEN) {
            transitionTo(OpportunityStatus.QUOTED, actor);
        } else {
            touch(actor);
        }
    }

    public void transitionTo(OpportunityStatus target, UUID actor) {
        assertActive();
        if (target == null || target == status) {
            touch(actor);
            return;
        }
        if (isTerminal()) {
            throw new ValidationException("Cannot transition from terminal status: " + status);
        }
        if (target == OpportunityStatus.QUOTED
                && status != OpportunityStatus.OPEN
                && status != OpportunityStatus.QUOTED) {
            throw new ValidationException("Invalid status transition: " + status + " -> " + target);
        }
        if ((target == OpportunityStatus.WON || target == OpportunityStatus.LOST)
                && status == OpportunityStatus.CANCELLED) {
            throw new ValidationException("Invalid status transition: " + status + " -> " + target);
        }
        this.status = target;
        touch(actor);
    }

    public boolean isTerminal() {
        return TERMINAL.contains(status);
    }

    public void softDelete(UUID actor) {
        if (deletedAt != null) {
            return;
        }
        this.deletedAt = Instant.now();
        touch(actor);
    }

    private void touch(UUID actor) {
        this.updatedAt = Instant.now();
        this.updatedBy = actor;
    }
}
