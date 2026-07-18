package com.aisales.lead.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
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
@Table(name = "lead_quality_score")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadQualityScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    @Column(name = "budget_fit", length = 20)
    private String budgetFit;

    @Column(length = 20)
    private String timeline;

    @Column(name = "decision_maker", length = 20)
    private String decisionMaker;

    @Column(name = "competitor_awareness", length = 20)
    private String competitorAwareness;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> objections;

    @Column(name = "suggested_product")
    private String suggestedProduct;

    @Column(name = "next_action")
    private String nextAction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_llm_response", columnDefinition = "jsonb")
    private Map<String, Object> rawLlmResponse;

    @Column(name = "scored_at", nullable = false)
    private Instant scoredAt;
}
