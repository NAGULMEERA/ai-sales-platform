package com.aisales.integration.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "meta_page_binding")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetaPageBinding {

    @Id
    private UUID id;

    @Column(name = "page_id", nullable = false, length = 64, unique = true)
    private String pageId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String platform = "FACEBOOK";

    @Builder.Default
    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType = "FACEBOOK_LEAD_ADS";

    @Column(name = "campaign_default", length = 255)
    private String campaignDefault;

    @Column(name = "prompt_code", length = 100)
    private String promptCode;

    /** Optional override; otherwise global Graph access token from config is used. */
    @Column(name = "page_access_token", length = 512)
    private String pageAccessToken;

    @Builder.Default
    @Column(name = "qualification_variable_keys", nullable = false, length = 500)
    private String qualificationVariableKeys = "budget,location,timeline";

    @Builder.Default
    @Column(name = "voice_qualify_enabled", nullable = false)
    private boolean voiceQualifyEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    @Version
    private Long version;
}
