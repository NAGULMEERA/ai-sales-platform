package com.aisales.customer.domain.entity;

import com.aisales.common.contracts.customer.CustomerSourceType;
import com.aisales.common.contracts.customer.CustomerStatus;
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
@Table(name = "customers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String email;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CustomerStatus status = CustomerStatus.PROSPECT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private CustomerSourceType sourceType = CustomerSourceType.MANUAL;

    @Column(name = "source_lead_id")
    private UUID sourceLeadId;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

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

    public void assertActiveRecord() {
        if (deletedAt != null) {
            throw new ValidationException("Customer is deleted");
        }
    }

    public void assertHasContactMethod() {
        if (isBlank(phone) && isBlank(email)) {
            throw new ValidationException("Customer must have at least one contact method (phone or email)");
        }
    }

    public void updateDetails(String name, String phoneValue, String emailValue,
                              CustomerStatus statusValue, Map<String, Object> metadataValue, UUID actor) {
        assertActiveRecord();
        if (status == CustomerStatus.ARCHIVED && statusValue != null && statusValue != CustomerStatus.ARCHIVED) {
            throw new ValidationException("Archived customer cannot change status without restore flow");
        }
        if (!isBlank(name)) {
            this.fullName = name.trim();
        }
        if (phoneValue != null) {
            this.phone = isBlank(phoneValue) ? null : phoneValue.trim();
        }
        if (emailValue != null) {
            this.email = isBlank(emailValue) ? null : emailValue.trim();
        }
        if (statusValue != null) {
            this.status = statusValue;
        }
        if (metadataValue != null) {
            this.metadata = new HashMap<>(metadataValue);
        }
        assertHasContactMethod();
        touch(actor);
    }

    public void activate(UUID actor) {
        assertActiveRecord();
        if (status == CustomerStatus.ARCHIVED) {
            throw new ValidationException("Cannot activate an archived customer");
        }
        this.status = CustomerStatus.ACTIVE;
        touch(actor);
    }

    public void archive(UUID actor) {
        assertActiveRecord();
        this.status = CustomerStatus.ARCHIVED;
        touch(actor);
    }

    public void softDelete(UUID actor) {
        if (deletedAt != null) {
            return;
        }
        this.deletedAt = Instant.now();
        this.status = CustomerStatus.ARCHIVED;
        touch(actor);
    }

    private void touch(UUID actor) {
        this.updatedAt = Instant.now();
        this.updatedBy = actor;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
