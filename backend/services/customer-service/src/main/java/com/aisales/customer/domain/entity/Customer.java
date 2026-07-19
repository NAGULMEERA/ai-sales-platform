package com.aisales.customer.domain.entity;

import com.aisales.common.contracts.customer.ContactMethodType;
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
import java.time.LocalDate;
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

    @Column(name = "customer_number", insertable = false, updatable = false)
    private String customerNumber;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String whatsapp;

    @Column(name = "external_crm_id")
    private String externalCrmId;

    @Column(name = "government_id")
    private String governmentId;

    @Column(length = 30)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String language;

    @Column(name = "preferred_channel", length = 50)
    private String preferredChannel;

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

    @Column(name = "merged_into_customer_id")
    private UUID mergedIntoCustomerId;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> preferences = new HashMap<>();

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
        if (isBlank(phone) && isBlank(email) && isBlank(whatsapp)) {
            throw new ValidationException(
                    "Customer must have at least one contact method (phone, email, or WhatsApp)");
        }
    }

    public void updateProfile(
            String name,
            String genderValue,
            LocalDate dob,
            String languageValue,
            Map<String, Object> metadataValue,
            UUID actor) {
        assertActiveRecord();
        if (!isBlank(name)) {
            this.fullName = name.trim();
        }
        if (genderValue != null) {
            this.gender = isBlank(genderValue) ? null : genderValue.trim();
        }
        if (dob != null) {
            this.dateOfBirth = dob;
        }
        if (languageValue != null) {
            this.language = isBlank(languageValue) ? null : languageValue.trim();
        }
        if (metadataValue != null) {
            this.metadata = new HashMap<>(metadataValue);
        }
        touch(actor);
    }

    public void updateContactDetails(
            String phoneValue, String emailValue, String whatsappValue, String externalCrmIdValue,
            String governmentIdValue, UUID actor) {
        assertActiveRecord();
        if (phoneValue != null) {
            this.phone = isBlank(phoneValue) ? null : phoneValue.trim();
        }
        if (emailValue != null) {
            this.email = isBlank(emailValue) ? null : emailValue.trim();
        }
        if (whatsappValue != null) {
            this.whatsapp = isBlank(whatsappValue) ? null : whatsappValue.trim();
        }
        if (externalCrmIdValue != null) {
            this.externalCrmId = isBlank(externalCrmIdValue) ? null : externalCrmIdValue.trim();
        }
        if (governmentIdValue != null) {
            this.governmentId = isBlank(governmentIdValue) ? null : governmentIdValue.trim();
        }
        assertHasContactMethod();
        touch(actor);
    }

    public void updateDetails(
            String name,
            String phoneValue,
            String emailValue,
            CustomerStatus statusValue,
            Map<String, Object> metadataValue,
            UUID actor) {
        updateProfile(name, null, null, null, metadataValue, actor);
        updateContactDetails(phoneValue, emailValue, null, null, null, actor);
        if (statusValue != null) {
            if (status == CustomerStatus.ARCHIVED && statusValue != CustomerStatus.ARCHIVED) {
                throw new ValidationException("Archived customer cannot change status without restore flow");
            }
            this.status = statusValue;
            touch(actor);
        }
    }

    public void updatePreferences(String preferredChannelValue, String languageValue,
                                  Map<String, Object> preferencesValue, UUID actor) {
        assertActiveRecord();
        if (preferredChannelValue != null) {
            this.preferredChannel = isBlank(preferredChannelValue) ? null : preferredChannelValue.trim();
        }
        if (languageValue != null) {
            this.language = isBlank(languageValue) ? null : languageValue.trim();
        }
        if (preferencesValue != null) {
            this.preferences = new HashMap<>(preferencesValue);
        }
        touch(actor);
    }

    public void syncPrimaryContact(ContactMethodType methodType, String value, UUID actor) {
        assertActiveRecord();
        if (methodType == null || isBlank(value)) {
            return;
        }
        String normalized = value.trim();
        switch (methodType) {
            case PHONE -> this.phone = normalized;
            case EMAIL -> this.email = normalized;
            case WHATSAPP -> this.whatsapp = normalized;
            default -> {
                // social accounts stay in contact methods / metadata
            }
        }
        assertHasContactMethod();
        touch(actor);
    }

    public void activate(UUID actor) {
        assertActiveRecord();
        if (status == CustomerStatus.ARCHIVED) {
            throw new ValidationException("Cannot activate an archived customer; restore first");
        }
        this.status = CustomerStatus.ACTIVE;
        touch(actor);
    }

    public void deactivate(UUID actor) {
        assertActiveRecord();
        if (status == CustomerStatus.ARCHIVED) {
            throw new ValidationException("Archived customer cannot be deactivated");
        }
        this.status = CustomerStatus.INACTIVE;
        touch(actor);
    }

    public void reactivate(UUID actor) {
        assertActiveRecord();
        if (status != CustomerStatus.INACTIVE && status != CustomerStatus.ARCHIVED) {
            throw new ValidationException("Only INACTIVE or ARCHIVED customers can be reactivated");
        }
        this.status = CustomerStatus.ACTIVE;
        touch(actor);
    }

    public void archive(UUID actor) {
        assertActiveRecord();
        this.status = CustomerStatus.ARCHIVED;
        touch(actor);
    }

    public void linkLead(UUID leadId, UUID actor) {
        assertActiveRecord();
        if (leadId == null) {
            throw new ValidationException("leadId is required");
        }
        if (sourceLeadId != null && !sourceLeadId.equals(leadId)) {
            throw new ValidationException("Customer already linked to a different lead");
        }
        this.sourceLeadId = leadId;
        if (sourceType == null || sourceType == CustomerSourceType.MANUAL) {
            this.sourceType = CustomerSourceType.LEAD_CONVERSION;
        }
        touch(actor);
    }

    /**
     * Absorbs identity/profile fields from a duplicate loser. Timeline/interactions are
     * re-pointed by the application service; this method never deletes history.
     */
    public void absorbFrom(Customer other, UUID actor) {
        assertActiveRecord();
        if (other == null) {
            throw new ValidationException("Merge source customer is required");
        }
        if (!tenantId.equals(other.getTenantId())) {
            throw new ValidationException("Cannot merge customers across tenants");
        }
        if (isBlank(this.phone) && !isBlank(other.getPhone())) {
            this.phone = other.getPhone();
        }
        if (isBlank(this.email) && !isBlank(other.getEmail())) {
            this.email = other.getEmail();
        }
        if (isBlank(this.whatsapp) && !isBlank(other.getWhatsapp())) {
            this.whatsapp = other.getWhatsapp();
        }
        if (isBlank(this.externalCrmId) && !isBlank(other.getExternalCrmId())) {
            this.externalCrmId = other.getExternalCrmId();
        }
        if (isBlank(this.governmentId) && !isBlank(other.getGovernmentId())) {
            this.governmentId = other.getGovernmentId();
        }
        if (isBlank(this.fullName) && !isBlank(other.getFullName())) {
            this.fullName = other.getFullName();
        }
        if (isBlank(this.gender) && !isBlank(other.getGender())) {
            this.gender = other.getGender();
        }
        if (this.dateOfBirth == null && other.getDateOfBirth() != null) {
            this.dateOfBirth = other.getDateOfBirth();
        }
        if (isBlank(this.language) && !isBlank(other.getLanguage())) {
            this.language = other.getLanguage();
        }
        if (isBlank(this.preferredChannel) && !isBlank(other.getPreferredChannel())) {
            this.preferredChannel = other.getPreferredChannel();
        }
        if (this.sourceLeadId == null && other.getSourceLeadId() != null) {
            this.sourceLeadId = other.getSourceLeadId();
        }
        if (other.getPreferences() != null && !other.getPreferences().isEmpty()) {
            Map<String, Object> mergedPrefs = new HashMap<>(
                    this.preferences != null ? this.preferences : Map.of());
            other.getPreferences().forEach(mergedPrefs::putIfAbsent);
            this.preferences = mergedPrefs;
        }
        if (other.getMetadata() != null && !other.getMetadata().isEmpty()) {
            Map<String, Object> mergedMeta = new HashMap<>(
                    this.metadata != null ? this.metadata : Map.of());
            other.getMetadata().forEach(mergedMeta::putIfAbsent);
            this.metadata = mergedMeta;
        }
        assertHasContactMethod();
        touch(actor);
    }

    public void markMergedInto(UUID survivorId, UUID actor) {
        if (survivorId == null) {
            throw new ValidationException("survivorId is required");
        }
        this.mergedIntoCustomerId = survivorId;
        this.status = CustomerStatus.ARCHIVED;
        this.deletedAt = Instant.now();
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
