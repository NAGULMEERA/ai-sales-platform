package com.aisales.deal.domain.entity;

import com.aisales.common.contracts.deal.QuoteStatus;
import com.aisales.common.exception.exception.ValidationException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quote")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quote {

    private static final Set<QuoteStatus> IMMUTABLE =
            EnumSet.of(QuoteStatus.SENT, QuoteStatus.ACCEPTED, QuoteStatus.REJECTED, QuoteStatus.SUPERSEDED);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "opportunity_id", nullable = false, updatable = false)
    private UUID opportunityId;

    @Builder.Default
    @Column(name = "quote_version", nullable = false)
    private Integer quoteVersion = 1;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuoteStatus status = QuoteStatus.DRAFT;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(length = 2000)
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<QuoteLineItem> lineItems = new ArrayList<>();

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
            throw new ValidationException("Quote is deleted");
        }
    }

    public void assertDraft() {
        assertActive();
        if (status != QuoteStatus.DRAFT) {
            throw new ValidationException("Only DRAFT quotes can be modified");
        }
    }

    public void addLineItem(QuoteLineItem item) {
        assertDraft();
        item.setQuote(this);
        item.setTenantId(this.tenantId);
        lineItems.add(item);
        recalculateTotal();
    }

    public void recalculateTotal() {
        this.totalAmount = lineItems.stream()
                .map(QuoteLineItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void send(UUID actor) {
        assertActive();
        if (status != QuoteStatus.DRAFT) {
            throw new ValidationException("Only DRAFT quotes can be sent");
        }
        if (lineItems.isEmpty()) {
            throw new ValidationException("Quote must have at least one line item");
        }
        this.status = QuoteStatus.SENT;
        touch(actor);
    }

    public void accept(UUID actor) {
        assertActive();
        if (status != QuoteStatus.SENT) {
            throw new ValidationException("Only SENT quotes can be accepted");
        }
        this.status = QuoteStatus.ACCEPTED;
        touch(actor);
    }

    public void supersede(UUID actor) {
        assertActive();
        if (status == QuoteStatus.ACCEPTED) {
            throw new ValidationException("Accepted quotes cannot be superseded");
        }
        if (status == QuoteStatus.DRAFT || status == QuoteStatus.SENT) {
            this.status = QuoteStatus.SUPERSEDED;
            touch(actor);
        }
    }

    public boolean isImmutable() {
        return IMMUTABLE.contains(status);
    }

    private void touch(UUID actor) {
        this.updatedAt = Instant.now();
        this.updatedBy = actor;
    }
}
