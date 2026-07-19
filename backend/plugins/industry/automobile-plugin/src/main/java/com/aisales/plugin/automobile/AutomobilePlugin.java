package com.aisales.plugin.automobile;

import com.aisales.plugin.contract.IndustryPlugin;
import com.aisales.plugin.contract.PipelineTemplateConfigs;
import com.aisales.plugin.contract.PluginDescriptor;
import com.aisales.plugin.contract.PluginType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Industry metadata plugin for Automobile.
 * Contributes suggested catalog/lead attribute keys and pipeline template metadata.
 * Does not own an industry microservice or Lead subtypes.
 */
public class AutomobilePlugin implements IndustryPlugin {

    public static final String PLUGIN_KEY = "automobile";
    public static final String PIPELINE_TEMPLATE_RESOURCE = "pipeline-templates/AUTOMOBILE_SALES_V1.json";

    public static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder()
            .pluginKey(PLUGIN_KEY)
            .type(PluginType.INDUSTRY)
            .version("1.0.0")
            .minPlatformVersion("1.0.0")
            .displayName("Automobile Industry")
            .description("Industry metadata: suggested catalog/lead attribute keys and pipeline template references. No industry microservice.")
            .capabilities(List.of(
                    "industry.automobile", "catalog.attributes", "catalog.match",
                    "pipeline.template", "deal.quote", "conversation.followup"))
            .industryCode("AUTOMOBILE")
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
        config.put("defaultPipelineCode", "AUTOMOBILE_SALES_V1");
        config.put("pipelineHappyPath", List.of("New", "Qualified", "Test Drive", "Quotation", "Finance", "Booked"));
        config.put("pipelineTemplate", PipelineTemplateConfigs.load(AutomobilePlugin.class, PIPELINE_TEMPLATE_RESOURCE));
        config.put("catalogAttributeKeys", List.of("make", "model", "year", "price"));
        config.put("matchAttributeKeys", List.of("make", "model", "year"));
        config.put("catalogOfferCategory", "vehicle");
        config.put("quoteLineSource", "catalog.offerId");
        config.put("leadAttributeKeys", List.of("vehicle", "budget", "financeRequired", "exchange"));
        config.put("qualificationPromptCode", "LEAD_QUALIFY_AUTOMOBILE");
        config.put("qualificationVariableKeys", List.of("budget", "vehicle", "financeRequired", "exchange"));
        config.put("conversationFollowupWorkflowKey", "CONVERSATION_FOLLOWUP_V1");
        config.put("defaultFollowupType", "TEST_DRIVE_FOLLOWUP");
        config.put("conversationSubjectTemplate", "Test-drive follow-up");
        config.put("inPersonEngagementLabel", "Test drive");
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
