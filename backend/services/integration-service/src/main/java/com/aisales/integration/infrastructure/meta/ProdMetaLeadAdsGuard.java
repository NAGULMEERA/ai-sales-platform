package com.aisales.integration.infrastructure.meta;

import com.aisales.integration.infrastructure.configuration.IntegrationServiceAuthProperties;
import com.aisales.integration.infrastructure.configuration.MetaLeadAdsProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fail-fast in production when Meta Lead Ads is enabled without verify/secret/Graph/service auth.
 */
@Component
@Profile("prod")
public class ProdMetaLeadAdsGuard implements ApplicationRunner {

    private final MetaLeadAdsProperties leadAdsProperties;
    private final IntegrationServiceAuthProperties serviceAuthProperties;

    public ProdMetaLeadAdsGuard(
            MetaLeadAdsProperties leadAdsProperties, IntegrationServiceAuthProperties serviceAuthProperties) {
        this.leadAdsProperties = leadAdsProperties;
        this.serviceAuthProperties = serviceAuthProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!leadAdsProperties.isEnabled()) {
            return;
        }
        if ("STUB".equalsIgnoreCase(leadAdsProperties.getMode())) {
            throw new IllegalStateException(
                    "aisales.integration.meta.lead-ads.mode=STUB is forbidden in prod. "
                            + "Set mode=LIVE before deploying.");
        }
        if (!StringUtils.hasText(leadAdsProperties.getVerifyToken())
                || !StringUtils.hasText(leadAdsProperties.getAppSecret())) {
            throw new IllegalStateException(
                    "Meta Lead Ads verify-token and app-secret are required in prod");
        }
        if (!StringUtils.hasText(leadAdsProperties.getGraph().getAccessToken())) {
            throw new IllegalStateException(
                    "aisales.integration.meta.lead-ads.graph.access-token is required in prod for Graph lead fetch");
        }
        if (!StringUtils.hasText(serviceAuthProperties.getBearerToken())) {
            throw new IllegalStateException(
                    "aisales.integration.service-auth.bearer-token is required in prod for Lead Service calls");
        }
    }
}
