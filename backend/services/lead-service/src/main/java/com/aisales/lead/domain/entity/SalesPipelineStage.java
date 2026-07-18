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
@Table(name = "sales_pipeline_stage")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesPipelineStage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pipeline_id", nullable = false, updatable = false)
    private UUID pipelineId;

    @Column(name = "stage_code", nullable = false, length = 50)
    private String stageCode;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "stage_order", nullable = false)
    private int stageOrder;

    @Builder.Default
    @Column(nullable = false)
    private boolean terminal = false;
}
