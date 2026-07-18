package com.aisales.plugin.automobile;

import com.aisales.plugin.contract.IndustryPlugin;
import com.aisales.plugin.contract.PluginDescriptor;
import com.aisales.plugin.contract.PluginType;
import static java.util.Map.entry;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Industry metadata plugin for Automobile.
 * Contributes suggested catalog/lead attribute keys and pipeline template references.
 * Does not own an industry microservice or Lead subtypes.
 */
public class AutomobilePlugin implements IndustryPlugin {

    public static final String PLUGIN_KEY = "automobile";

    public static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder()
            .pluginKey(PLUGIN_KEY)
            .type(PluginType.INDUSTRY)
            .version("1.0.0")
            .displayName("Automobile Industry")
            .description("Industry metadata: suggested catalog/lead attribute keys and pipeline template references. No industry microservice.")
            .capabilities(List.of(
                    "industry.automobile", "catalog.attributes", "catalog.match",
                    "pipeline.template", "deal.quote", "conversation.followup"))
            .industryCode("AUTOMOBILE")
            .configSchemaJson("""
                    {"type":"object","properties":{"defaultPipelineCode":{"type":"string"},"catalogAttributeKeys":{"type":"array","items":{"type":"string"}},"matchAttributeKeys":{"type":"array","items":{"type":"string"}},"catalogOfferCategory":{"type":"string"},"quoteLineSource":{"type":"string"},"leadAttributeKeys":{"type":"array","items":{"type":"string"}},"qualificationPromptCode":{"type":"string"},"qualificationVariableKeys":{"type":"array","items":{"type":"string"}},"conversationFollowupWorkflowKey":{"type":"string"},"defaultFollowupType":{"type":"string"},"conversationSubjectTemplate":{"type":"string"},"inPersonEngagementLabel":{"type":"string"}}}
                    """)
            .defaultConfig(Map.ofEntries(
                    entry("defaultPipelineCode", "AUTOMOBILE_SALES_V1"),
                    entry("pipelineHappyPath", List.of("New", "Qualified", "Test Drive", "Quotation", "Finance", "Booked")),
                    entry("catalogAttributeKeys", List.of("make", "model", "year", "price")),
                    entry("matchAttributeKeys", List.of("make", "model", "year")),
                    entry("catalogOfferCategory", "vehicle"),
                    entry("quoteLineSource", "catalog.offerId"),
                    entry("leadAttributeKeys", List.of("vehicle", "budget", "financeRequired", "exchange")),
                    entry("qualificationPromptCode", "LEAD_QUALIFY_AUTOMOBILE"),
                    entry("qualificationVariableKeys", List.of("budget", "vehicle", "financeRequired", "exchange")),
                    entry("conversationFollowupWorkflowKey", "CONVERSATION_FOLLOWUP_V1"),
                    entry("defaultFollowupType", "TEST_DRIVE_FOLLOWUP"),
                    entry("conversationSubjectTemplate", "Test-drive follow-up"),
                    entry("inPersonEngagementLabel", "Test drive")))
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
