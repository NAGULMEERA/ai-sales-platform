package com.aisales.lead.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sales_pipeline_transition")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesPipelineTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pipeline_id", nullable = false, updatable = false)
    private UUID pipelineId;

    @Column(name = "from_stage", nullable = false, length = 50)
    private String fromStage;

    @Column(name = "to_stage", nullable = false, length = 50)
    private String toStage;
}
