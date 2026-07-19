package com.aisales.lead.domain.entity;

import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.lead.domain.service.LeadStateMachine;
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
@Table(name = "leads")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "organization_id")
    private UUID organizationId;

    /** Tenant sales pipeline this lead follows. Null → platform default graph at transition time. */
    @Column(name = "pipeline_id")
    private UUID pipelineId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "external_id", insertable = false, updatable = false)
    private String externalId;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_id")
    private String sourceId;

    @Column(length = 255)
    private String campaign;

    @Column(name = "utm_source")
    private String utmSource;

    @Column(name = "utm_campaign")
    private String utmCampaign;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(length = 255)
    private String email;

    @Builder.Default
    @Column(nullable = false)
    private boolean validated = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean qualified = false;

    private Integer score;

    @Column(name = "confidence_score")
    private Integer confidenceScore;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "lead_status")
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    /**
     * Industry-agnostic attributes (plugin-defined keys). Persisted as leads.metadata.
     */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
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
            throw new ValidationException("Lead is deleted");
        }
    }

    public void assertHasContactMethod() {
        if (isBlank(phone) && isBlank(email)) {
            throw new ValidationException("Lead must have at least one contact method (phone or email)");
        }
    }

    public void markValidated(UUID actor) {
        assertActive();
        assertHasContactMethod();
        this.validated = true;
        touch(actor);
    }

    public void assign(UUID assignee, UUID actor, LeadStateMachine stateMachine) {
        assertActive();
        if (stateMachine.isTerminal(status)) {
            throw new ValidationException("Cannot assign a terminal lead");
        }
        if (!validated) {
            throw new ValidationException("Lead must be validated before assignment");
        }
        if (assignee == null) {
            throw new ValidationException("Assignee is required");
        }
        this.assignedTo = assignee;
        touch(actor);
    }

    public void unassign(UUID actor, LeadStateMachine stateMachine) {
        assertActive();
        if (stateMachine.isTerminal(status)) {
            throw new ValidationException("Cannot unassign a terminal lead");
        }
        if (assignedTo == null) {
            throw new ValidationException("Lead is not assigned");
        }
        this.assignedTo = null;
        touch(actor);
    }

    /**
     * Reopens a lost lead into an active pipeline stage (default QUALIFIED).
     */
    public void reopen(LeadStatus target, UUID actor, LeadStateMachine stateMachine) {
        assertActive();
        if (status != LeadStatus.LOST) {
            throw new ValidationException("Only LOST leads can be reopened");
        }
        LeadStatus destination = target != null ? target : LeadStatus.QUALIFIED;
        if (destination == LeadStatus.LOST
                || destination == LeadStatus.WON
                || destination == LeadStatus.ARCHIVED) {
            throw new ValidationException("Invalid reopen target: " + destination);
        }
        stateMachine.assertTransition(pipelineId, status, destination);
        this.status = destination;
        if (destination == LeadStatus.QUALIFIED) {
            this.qualified = true;
        }
        touch(actor);
    }

    /** Restores a soft-deleted lead. */
    public void restore(UUID actor) {
        if (deletedAt == null) {
            throw new ValidationException("Lead is not deleted");
        }
        this.deletedAt = null;
        touch(actor);
    }

    /**
     * Absorbs contact/details from a duplicate loser into this survivor (merge).
     * Does not change status or assignment of the survivor.
     */
    public void absorbFrom(Lead other, UUID actor) {
        assertActive();
        if (other == null) {
            throw new ValidationException("Merge source lead is required");
        }
        if (!tenantId.equals(other.getTenantId())) {
            throw new ValidationException("Cannot merge leads across tenants");
        }
        if (isBlank(this.email) && !isBlank(other.getEmail())) {
            this.email = other.getEmail();
        }
        if (isBlank(this.phone) && !isBlank(other.getPhone())) {
            this.phone = other.getPhone();
        }
        if (isBlank(this.customerName) && !isBlank(other.getCustomerName())) {
            this.customerName = other.getCustomerName();
        }
        if (this.customerId == null && other.getCustomerId() != null) {
            this.customerId = other.getCustomerId();
        }
        if (other.getAttributes() != null && !other.getAttributes().isEmpty()) {
            Map<String, Object> merged = new HashMap<>(
                    this.attributes != null ? this.attributes : Map.of());
            other.getAttributes().forEach(merged::putIfAbsent);
            this.attributes = merged;
        }
        if (this.score == null && other.getScore() != null) {
            this.score = other.getScore();
        }
        if (this.confidenceScore == null && other.getConfidenceScore() != null) {
            this.confidenceScore = other.getConfidenceScore();
        }
        assertHasContactMethod();
        touch(actor);
    }

    public void qualify(Integer newScore, UUID actor, LeadStateMachine stateMachine) {
        assertActive();
        LeadStatus previous = this.status;
        if (previous != LeadStatus.QUALIFIED) {
            stateMachine.assertTransition(pipelineId, previous, LeadStatus.QUALIFIED);
            this.status = LeadStatus.QUALIFIED;
        }
        this.qualified = true;
        if (newScore != null) {
            applyScore(newScore, actor, stateMachine);
        } else {
            touch(actor);
        }
    }

    public void markContacted(UUID actor, LeadStateMachine stateMachine) {
        assertActive();
        if (status != LeadStatus.CONTACTED) {
            stateMachine.assertTransition(pipelineId, status, LeadStatus.CONTACTED);
            this.status = LeadStatus.CONTACTED;
        }
        touch(actor);
    }

    public void transitionTo(LeadStatus target, UUID actor, LeadStateMachine stateMachine) {
        assertActive();
        stateMachine.assertTransition(pipelineId, status, target);
        this.status = target;
        if (target == LeadStatus.QUALIFIED) {
            this.qualified = true;
        }
        if (target == LeadStatus.WON || target == LeadStatus.LOST) {
            // terminal
        }
        touch(actor);
    }

    public void convert(UUID customerIdValue, UUID actor, LeadStateMachine stateMachine) {
        assertActive();
        if (customerIdValue == null) {
            throw new ValidationException("customerId is required to convert a lead");
        }
        if (status != LeadStatus.WON) {
            stateMachine.assertTransition(pipelineId, status, LeadStatus.WON);
            this.status = LeadStatus.WON;
        }
        this.customerId = customerIdValue;
        touch(actor);
    }

    public void lose(UUID actor, LeadStateMachine stateMachine) {
        assertActive();
        if (status != LeadStatus.LOST) {
            stateMachine.assertTransition(pipelineId, status, LeadStatus.LOST);
            this.status = LeadStatus.LOST;
        }
        touch(actor);
    }

    public void scheduleVisit(UUID actor, LeadStateMachine stateMachine) {
        assertActive();
        if (status != LeadStatus.APPOINTMENT_BOOKED) {
            stateMachine.assertTransition(pipelineId, status, LeadStatus.APPOINTMENT_BOOKED);
            this.status = LeadStatus.APPOINTMENT_BOOKED;
        }
        touch(actor);
    }

    public void completeVisit(UUID actor, LeadStateMachine stateMachine) {
        assertActive();
        if (status != LeadStatus.VISITED) {
            stateMachine.assertTransition(pipelineId, status, LeadStatus.VISITED);
            this.status = LeadStatus.VISITED;
        }
        touch(actor);
    }

    public void cancelVisit(UUID actor, LeadStateMachine stateMachine) {
        assertActive();
        if (status != LeadStatus.APPOINTMENT_BOOKED) {
            throw new ValidationException("Only a scheduled visit can be cancelled");
        }
        stateMachine.assertTransition(pipelineId, status, LeadStatus.QUALIFIED);
        this.status = LeadStatus.QUALIFIED;
        touch(actor);
    }

    public void archive(UUID actor, LeadStateMachine stateMachine) {
        assertActive();
        if (status != LeadStatus.ARCHIVED) {
            stateMachine.assertTransition(pipelineId, status, LeadStatus.ARCHIVED);
            this.status = LeadStatus.ARCHIVED;
        }
        touch(actor);
    }

    public void applyScore(int newScore, UUID actor, LeadStateMachine stateMachine) {
        assertActive();
        if (stateMachine.isTerminal(status)) {
            throw new ValidationException("Cannot score a terminal lead");
        }
        if (newScore < 0 || newScore > 100) {
            throw new ValidationException("Score must be between 0 and 100");
        }
        this.score = newScore;
        touch(actor);
    }

    public void applyConfidenceScore(Integer confidence, UUID actor) {
        if (confidence == null) {
            return;
        }
        if (confidence < 0 || confidence > 100) {
            throw new ValidationException("Confidence score must be between 0 and 100");
        }
        this.confidenceScore = confidence;
        touch(actor);
    }

    public void updateDetails(String name, String phoneValue, String emailValue,
                              String sourceTypeValue, String sourceIdValue, String campaignValue,
                              Map<String, Object> attributesValue, UUID actor) {
        assertActive();
        if (!isBlank(name)) {
            this.customerName = name.trim();
        }
        if (phoneValue != null) {
            this.phone = phoneValue.trim();
        }
        if (emailValue != null) {
            this.email = isBlank(emailValue) ? null : emailValue.trim();
        }
        if (!isBlank(sourceTypeValue)) {
            this.sourceType = sourceTypeValue.trim();
        }
        if (sourceIdValue != null) {
            this.sourceId = sourceIdValue;
        }
        if (campaignValue != null) {
            this.campaign = campaignValue;
        }
        if (attributesValue != null) {
            this.attributes = new HashMap<>(attributesValue);
        }
        assertHasContactMethod();
        touch(actor);
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
