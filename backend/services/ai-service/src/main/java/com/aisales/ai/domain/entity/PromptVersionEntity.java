package com.aisales.ai.domain.entity;

import com.aisales.common.contracts.ai.PromptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "prompt_version")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "prompt_id", nullable = false, updatable = false)
    private UUID promptId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "system_template", columnDefinition = "TEXT")
    private String systemTemplate;

    @Column(name = "user_template", nullable = false, columnDefinition = "TEXT")
    private String userTemplate;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> variables = new ArrayList<>();

    @Column(name = "expected_output_hint", length = 2000)
    private String expectedOutputHint;

    @Column(length = 2000)
    private String changelog;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromptStatus status = PromptStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;
}
