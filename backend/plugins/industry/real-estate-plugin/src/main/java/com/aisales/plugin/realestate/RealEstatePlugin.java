package com.aisales.plugin.realestate;

import com.aisales.plugin.contract.IndustryPlugin;
import com.aisales.plugin.contract.PipelineTemplateConfigs;
import com.aisales.plugin.contract.PluginDescriptor;
import com.aisales.plugin.contract.PluginType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Industry metadata plugin for Real Estate.
 * Contributes suggested catalog attribute keys and pipeline template metadata.
 * Does not own an industry microservice or Lead subtypes.
 */
public class RealEstatePlugin implements IndustryPlugin {

    public static final String PLUGIN_KEY = "real-estate";
    public static final String PIPELINE_TEMPLATE_RESOURCE = "pipeline-templates/REAL_ESTATE_SALES_V1.json";

    public static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder()
            .pluginKey(PLUGIN_KEY)
            .type(PluginType.INDUSTRY)
            .version("1.0.0")
            .minPlatformVersion("1.0.0")
            .displayName("Real Estate Industry")
            .description("Industry metadata: suggested catalog attribute keys and pipeline template references. No industry microservice.")
            .capabilities(List.of(
                    "industry.real_estate", "catalog.attributes", "catalog.match",
                    "pipeline.template", "deal.quote", "conversation.followup"))
            .industryCode("REAL_ESTATE")
            .configSchemaJson("""
                    {"type":"object","properties":{"defaultPipelineCode":{"type":"string"},"pipelineTemplate":{"type":"object"},"catalogAttributeKeys":{"type":"array","items":{"type":"string"}},"matchAttributeKeys":{"type":"array","items":{"type":"string"}},"catalogOfferCategory":{"type":"string"},"quoteLineSource":{"type":"string"},"leadAttributeKeys":{"type":"array","items":{"type":"string"}},"qualificationPromptCode":{"type":"string"},"qualificationVariableKeys":{"type":"array","items":{"type":"string"}},"conversationFollowupWorkflowKey":{"type":"string"},"defaultFollowupType":{"type":"string"},"conversationSubjectTemplate":{"type":"string"},"inPersonEngagementLabel":{"type":"string"}}}
                    """)
            .defaultConfig(defaultConfig())
            .metadata(Map.of(
                    "ownsMicroservice", false,
                    "leadSubtype", false,
                    "industryQuoteType", false,
                    "industryConversationType", false))
            .build();

    private static Map<String, Object> defaultConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("defaultPipelineCode", "REAL_ESTATE_SALES_V1");
        config.put("pipelineHappyPath", List.of("New", "Qualified", "Visit", "Negotiation", "Booked"));
        config.put("pipelineTemplate", PipelineTemplateConfigs.load(RealEstatePlugin.class, PIPELINE_TEMPLATE_RESOURCE));
        config.put("catalogAttributeKeys", List.of("bedrooms", "bathrooms", "location", "price"));
        config.put("matchAttributeKeys", List.of("bedrooms", "bathrooms", "location"));
        config.put("catalogOfferCategory", "residential");
        config.put("quoteLineSource", "catalog.offerId");
        config.put("leadAttributeKeys", List.of("budget", "location", "propertyType", "timeline"));
        config.put("qualificationPromptCode", "LEAD_QUALIFY_REAL_ESTATE");
        config.put("qualificationVariableKeys", List.of("budget", "location", "timeline"));
        config.put("conversationFollowupWorkflowKey", "CONVERSATION_FOLLOWUP_V1");
        config.put("defaultFollowupType", "VISIT_FOLLOWUP");
        config.put("conversationSubjectTemplate", "Visit follow-up");
        config.put("inPersonEngagementLabel", "Site visit");
        return Map.copyOf(config);
    }

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
