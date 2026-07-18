package com.aisales.plugin.realestate;

import com.aisales.plugin.contract.IndustryPlugin;
import com.aisales.plugin.contract.PluginDescriptor;
import com.aisales.plugin.contract.PluginType;
import static java.util.Map.entry;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Industry metadata plugin for Real Estate.
 * Contributes suggested catalog attribute keys and pipeline template references.
 * Does not own an industry microservice or Lead subtypes.
 */
public class RealEstatePlugin implements IndustryPlugin {

    public static final String PLUGIN_KEY = "real-estate";

    public static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder()
            .pluginKey(PLUGIN_KEY)
            .type(PluginType.INDUSTRY)
            .version("1.0.0")
            .displayName("Real Estate Industry")
            .description("Industry metadata: suggested catalog attribute keys and pipeline template references. No industry microservice.")
            .capabilities(List.of(
                    "industry.real_estate", "catalog.attributes", "catalog.match",
                    "pipeline.template", "deal.quote", "conversation.followup"))
            .industryCode("REAL_ESTATE")
            .configSchemaJson("""
                    {"type":"object","properties":{"defaultPipelineCode":{"type":"string"},"catalogAttributeKeys":{"type":"array","items":{"type":"string"}},"matchAttributeKeys":{"type":"array","items":{"type":"string"}},"catalogOfferCategory":{"type":"string"},"quoteLineSource":{"type":"string"},"leadAttributeKeys":{"type":"array","items":{"type":"string"}},"qualificationPromptCode":{"type":"string"},"qualificationVariableKeys":{"type":"array","items":{"type":"string"}},"conversationFollowupWorkflowKey":{"type":"string"},"defaultFollowupType":{"type":"string"},"conversationSubjectTemplate":{"type":"string"},"inPersonEngagementLabel":{"type":"string"}}}
                    """)
            .defaultConfig(Map.ofEntries(
                    entry("defaultPipelineCode", "REAL_ESTATE_SALES_V1"),
                    entry("pipelineHappyPath", List.of("New", "Qualified", "Visit", "Negotiation", "Booked")),
                    entry("catalogAttributeKeys", List.of("bedrooms", "bathrooms", "location", "price")),
                    entry("matchAttributeKeys", List.of("bedrooms", "bathrooms", "location")),
                    entry("catalogOfferCategory", "residential"),
                    entry("quoteLineSource", "catalog.offerId"),
                    entry("leadAttributeKeys", List.of("budget", "location", "propertyType", "timeline")),
                    entry("qualificationPromptCode", "LEAD_QUALIFY_REAL_ESTATE"),
                    entry("qualificationVariableKeys", List.of("budget", "location", "timeline")),
                    entry("conversationFollowupWorkflowKey", "CONVERSATION_FOLLOWUP_V1"),
                    entry("defaultFollowupType", "VISIT_FOLLOWUP"),
                    entry("conversationSubjectTemplate", "Visit follow-up"),
                    entry("inPersonEngagementLabel", "Site visit")))
            .metadata(Map.of(
                    "ownsMicroservice", false,
                    "leadSubtype", false,
                    "industryQuoteType", false,
                    "industryConversationType", false))
            .build();

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void onEnable(UUID tenantId) {
        // Enablement is persisted by marketplace-service. No business registration here.
    }

    @Override
    public void onDisable(UUID tenantId) {
        // Disablement is persisted by marketplace-service.
    }
}
