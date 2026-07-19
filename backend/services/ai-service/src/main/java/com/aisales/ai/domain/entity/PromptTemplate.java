package com.aisales.ai.domain.entity;

import com.aisales.common.contracts.ai.PromptStatus;
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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prompt_template")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 100)
    private String purpose;

    /** Optional industry key for dimension resolution (e.g. REAL_ESTATE). */
    @Column(name = "industry_code", length = 64)
    private String industryCode;

    /** Optional BCP-47 / ISO language (e.g. en, hi). Null = language-agnostic. */
    @Column(name = "language_code", length = 16)
    private String languageCode;

    /** Optional capability key (e.g. LEAD_QUALIFICATION, RAG_ANSWER). */
    @Column(name = "capability", length = 64)
    private String capability;

    /** Optional preferred model hint for routing (not a hard provider lock). */
    @Column(name = "preferred_model", length = 128)
    private String preferredModel;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromptStatus status = PromptStatus.DRAFT;

    @Column(name = "active_version")
    private Integer activeVersion;

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
            throw new ValidationException("Prompt is deleted");
        }
    }

    public void activateVersion(int versionNumber, UUID actor) {
        assertActiveRecord();
        this.activeVersion = versionNumber;
        this.status = PromptStatus.ACTIVE;
        touch(actor);
    }

    public void archive(UUID actor) {
        assertActiveRecord();
        this.status = PromptStatus.ARCHIVED;
        touch(actor);
    }

    private void touch(UUID actor) {
        this.updatedAt = Instant.now();
        this.updatedBy = actor;
    }
}
