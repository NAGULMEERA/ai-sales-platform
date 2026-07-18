package com.aisales.lead.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
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
@Table(name = "lead_attribution")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadAttribution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(nullable = false, length = 50)
    private String channel;

    private String campaign;

    @Column(name = "ad_id")
    private String adId;

    private Integer position;

    @Column(precision = 19, scale = 4)
    private BigDecimal cost;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_details", columnDefinition = "jsonb")
    private Map<String, Object> sourceDetails;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
