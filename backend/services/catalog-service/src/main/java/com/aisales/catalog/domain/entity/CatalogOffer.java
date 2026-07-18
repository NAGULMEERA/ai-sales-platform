package com.aisales.catalog.domain.entity;

import com.aisales.common.contracts.catalog.CatalogItemStatus;
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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "catalog_offer")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CatalogItemStatus status = CatalogItemStatus.ACTIVE;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

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
            throw new ValidationException("Catalog offer is deleted");
        }
    }

    public boolean isCurrentlyValid(Instant now) {
        if (status != CatalogItemStatus.ACTIVE || deletedAt != null) {
            return false;
        }
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }
        if (validTo != null && now.isAfter(validTo)) {
            return false;
        }
        return true;
    }

    public void update(String nameValue, String currencyValue, BigDecimal price,
                       CatalogItemStatus statusValue, Instant from, Instant to, UUID actor) {
        assertActive();
        if (nameValue != null && !nameValue.isBlank()) {
            this.name = nameValue.trim();
        }
        if (currencyValue != null && !currencyValue.isBlank()) {
            this.currency = currencyValue.trim().toUpperCase();
        }
        if (price != null) {
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Unit price must be >= 0");
            }
            this.unitPrice = price;
        }
        if (statusValue != null) {
            this.status = statusValue;
        }
        if (from != null || to != null) {
            this.validFrom = from;
            this.validTo = to;
        }
        touch(actor);
    }

    public void softDelete(UUID actor) {
        if (deletedAt != null) {
            return;
        }
        this.deletedAt = Instant.now();
        this.status = CatalogItemStatus.ARCHIVED;
        touch(actor);
    }

    private void touch(UUID actor) {
        this.updatedAt = Instant.now();
        this.updatedBy = actor;
    }
}
