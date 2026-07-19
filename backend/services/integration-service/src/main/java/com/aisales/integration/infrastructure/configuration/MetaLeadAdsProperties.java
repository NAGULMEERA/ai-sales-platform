package com.aisales.integration.infrastructure.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Meta Lead Ads ingest. Mode {@code STUB} accepts a simplified JSON payload for local/dev;
 * {@code LIVE} expects Meta webhook envelopes + Graph API lead fetch.
 */
@Data
@ConfigurationProperties(prefix = "aisales.integration.meta.lead-ads")
public class MetaLeadAdsProperties {

    private boolean enabled = true;

    /** STUB | LIVE */
    private String mode = "STUB";

    /** Meta webhook verify token (GET hub.challenge). */
    private String verifyToken = "";

    /** App secret for X-Hub-Signature-256. Empty skips verify in STUB only. */
    private String appSecret = "";

    private Graph graph = new Graph();

    @Data
    public static class Graph {
        private String baseUrl = "https://graph.facebook.com";
        private String apiVersion = "v21.0";
        /** System user / page access token (env {@code META_PAGE_ACCESS_TOKEN}). */
        private String accessToken = "";
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 15000;
    }
}
