package com.aisales.lead.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lead_duplicates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadDuplicate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "duplicate_of_lead_id", nullable = false)
    private UUID duplicateOfLeadId;

    @Column(name = "similarity_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal similarityScore;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean resolved = false;

    @Column(name = "merged_into_lead_id")
    private UUID mergedIntoLeadId;

    @Column(name = "merged_at")
    private Instant mergedAt;
}
