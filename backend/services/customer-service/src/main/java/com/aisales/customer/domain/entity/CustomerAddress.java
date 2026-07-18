package com.aisales.customer.domain.entity;

import com.aisales.common.exception.exception.ValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_addresses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Builder.Default
    @Column(name = "address_type", nullable = false, length = 50)
    private String addressType = "HOME";

    @Column(nullable = false)
    private String line1;

    private String line2;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Builder.Default
    @Column(nullable = false, length = 100)
    private String country = "India";

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private boolean primaryAddress = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public void assertActive() {
        if (deletedAt != null) {
            throw new ValidationException("Customer address is deleted");
        }
    }

    public void softDelete(UUID actor) {
        if (deletedAt != null) {
            return;
        }
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
        this.updatedBy = actor;
    }
}
