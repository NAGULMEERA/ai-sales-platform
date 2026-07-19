package com.aisales.integration.application.dto;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Local/dev Lead Ads payload (STUB mode). Mirrors form capture from FB/IG ads
 * without calling Meta Graph API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StubLeadAdsPayload {

    private String pageId;
    private String leadgenId;
    /** FACEBOOK | INSTAGRAM */
    private String platform;
    private String fullName;
    private String phone;
    private String email;
    private String campaign;

    @Builder.Default
    private Map<String, Object> fields = new HashMap<>();
}
