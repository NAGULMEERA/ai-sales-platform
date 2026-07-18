package com.aisales.catalog.domain.entity;

import com.aisales.common.contracts.catalog.CatalogItemStatus;
import com.aisales.common.contracts.catalog.CatalogProductType;
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
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "catalog_product")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 30)
    private CatalogProductType productType;

    @Column(length = 100)
    private String category;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CatalogItemStatus status = CatalogItemStatus.ACTIVE;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();

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
            throw new ValidationException("Catalog product is deleted");
        }
    }

    public void update(String nameValue, String descriptionValue, CatalogProductType type,
                       String categoryValue, CatalogItemStatus statusValue,
                       Map<String, Object> attributesValue, UUID actor) {
        assertActive();
        if (nameValue != null && !nameValue.isBlank()) {
            this.name = nameValue.trim();
        }
        if (descriptionValue != null) {
            this.description = descriptionValue.isBlank() ? null : descriptionValue.trim();
        }
        if (type != null) {
            this.productType = type;
        }
        if (categoryValue != null) {
            this.category = categoryValue.isBlank() ? null : categoryValue.trim();
        }
        if (statusValue != null) {
            this.status = statusValue;
        }
        if (attributesValue != null) {
            this.attributes = new HashMap<>(attributesValue);
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
