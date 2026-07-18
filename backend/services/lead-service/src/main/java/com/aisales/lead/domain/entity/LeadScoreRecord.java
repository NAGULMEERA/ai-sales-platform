package com.aisales.lead.domain.entity;

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
@Table(name = "lead_scores")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadScoreRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "score_type", nullable = false, length = 50)
    private String scoreType;

    private String explanation;

    @Column(name = "scored_at", nullable = false)
    private Instant scoredAt;

    @Column(name = "scored_by")
    private UUID scoredBy;
}
