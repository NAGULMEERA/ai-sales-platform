package com.aisales.plugin.naturalfarming;

import com.aisales.plugin.contract.IndustryPlugin;
import com.aisales.plugin.contract.PipelineTemplateConfigs;
import com.aisales.plugin.contract.PluginDescriptor;
import com.aisales.plugin.contract.PluginType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Industry metadata plugin for Natural Farming.
 * Contributes catalog/lead attribute keys, pipeline template, qualification prompt refs,
 * and WhatsApp/follow-up config. Does not own an industry microservice or domain tables.
 *
 * <p>Platform ownership mapping (reuse, do not duplicate):
 * <ul>
 *   <li>Farm / harvest / inventory attributes → catalog-service product/offer metadata</li>
 *   <li>Orders / quotes → deal-service opportunities + quotes</li>
 *   <li>Customers → customer-service</li>
 *   <li>WhatsApp lead capture → whatsapp-channel + integration-service</li>
 *   <li>AI recommend / qualify → ai-service + catalog match/recommend</li>
 *   <li>Order workflow → workflow-service</li>
 *   <li>Payment → billing-service</li>
 *   <li>Delivery follow-up → conversation + timeline labels</li>
 *   <li>Reports / dashboards → analytics-service + search-service</li>
 * </ul>
 */
public class NaturalFarmingPlugin implements IndustryPlugin {

    public static final String PLUGIN_KEY = "natural-farming";
    public static final String PIPELINE_TEMPLATE_RESOURCE = "pipeline-templates/NATURAL_FARMING_SALES_V1.json";

    public static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder()
            .pluginKey(PLUGIN_KEY)
            .type(PluginType.INDUSTRY)
            .version("1.0.0")
            .minPlatformVersion("1.0.0")
            .displayName("Natural Farming Industry")
            .description(
                    "Industry metadata for natural farming: farm/harvest/catalog attribute keys, "
                            + "sales pipeline, AI qualification & recommendation prompt refs, "
                            + "WhatsApp follow-up labels. No industry microservice.")
            .capabilities(List.of(
                    "industry.natural_farming",
                    "catalog.attributes",
                    "catalog.match",
                    "catalog.recommend",
                    "pipeline.template",
                    "deal.quote",
                    "conversation.followup",
                    "notification.whatsapp",
                    "workflow.order",
                    "billing.payment",
                    "search.projection",
                    "analytics.facts"))
            .industryCode("NATURAL_FARMING")
            .configSchemaJson("""
                    {"type":"object","properties":{"defaultPipelineCode":{"type":"string"},"pipelineTemplate":{"type":"object"},"catalogAttributeKeys":{"type":"array","items":{"type":"string"}},"matchAttributeKeys":{"type":"array","items":{"type":"string"}},"catalogOfferCategory":{"type":"string"},"quoteLineSource":{"type":"string"},"leadAttributeKeys":{"type":"array","items":{"type":"string"}},"qualificationPromptCode":{"type":"string"},"qualificationVariableKeys":{"type":"array","items":{"type":"string"}},"recommendationPromptCode":{"type":"string"},"conversationFollowupWorkflowKey":{"type":"string"},"defaultFollowupType":{"type":"string"},"conversationSubjectTemplate":{"type":"string"},"inPersonEngagementLabel":{"type":"string"},"orderWorkflowKey":{"type":"string"},"paymentCapabilityRef":{"type":"string"},"whatsappLeadCaptureEnabled":{"type":"boolean"},"knowledgeCategoryCode":{"type":"string"},"farmAttributeKeys":{"type":"array","items":{"type":"string"}},"harvestAttributeKeys":{"type":"array","items":{"type":"string"}},"inventoryAttributeKeys":{"type":"array","items":{"type":"string"}},"deliveryAttributeKeys":{"type":"array","items":{"type":"string"}}}}
                    """)
            .defaultConfig(defaultConfig())
            .metadata(pluginMetadata())
            .build();

    private static Map<String, Object> pluginMetadata() {
        Map<String, Object> owners = new LinkedHashMap<>();
        owners.put("farmManagement", "catalog-service (product attributes)");
        owners.put("productCatalog", "catalog-service");
        owners.put("harvestManagement", "catalog-service (offer/product attributes)");
        owners.put("inventory", "catalog-service (offer attributes: stockKg, availableFrom)");
        owners.put("orderManagement", "deal-service (opportunity + quote)");
        owners.put("customerManagement", "customer-service");
        owners.put("whatsappLeadCapture", "integration-service + whatsapp-channel");
        owners.put("aiRecommendation", "catalog-service recommend + ai-service prompts");
        owners.put("orderWorkflow", "workflow-service");
        owners.put("payment", "billing-service");
        owners.put("deliveryTracking", "conversation-service + opportunity timeline");
        owners.put("reportsDashboards", "analytics-service");
        owners.put("eventsSearch", "common-events + search-service");

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("ownsMicroservice", false);
        metadata.put("leadSubtype", false);
        metadata.put("industryQuoteType", false);
        metadata.put("industryConversationType", false);
        metadata.put("platformOwners", Map.copyOf(owners));
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> defaultConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("defaultPipelineCode", "NATURAL_FARMING_SALES_V1");
        config.put("pipelineHappyPath", List.of(
                "New", "Qualified", "Farm Visit", "Negotiation", "Order Confirmed", "Delivered"));
        config.put(
                "pipelineTemplate",
                PipelineTemplateConfigs.load(NaturalFarmingPlugin.class, PIPELINE_TEMPLATE_RESOURCE));

        // Product catalog / farm produce SKUs
        config.put(
                "catalogAttributeKeys",
                List.of(
                        "farmId",
                        "farmName",
                        "plotId",
                        "cropType",
                        "variety",
                        "season",
                        "harvestDate",
                        "yieldKg",
                        "unit",
                        "organicCertified",
                        "certificationBody",
                        "region",
                        "packaging",
                        "shelfLifeDays",
                        "stockKg",
                        "availableFrom",
                        "pricePerKg"));
        config.put("matchAttributeKeys", List.of("cropType", "region", "organicCertified", "season"));
        config.put("catalogOfferCategory", "natural-produce");
        config.put("quoteLineSource", "catalog.offerId");

        // Domain attribute groupings (still catalog/lead metadata — not separate aggregates)
        config.put("farmAttributeKeys", List.of("farmId", "farmName", "plotId", "soilType", "irrigationType", "region"));
        config.put(
                "harvestAttributeKeys",
                List.of("cropType", "variety", "season", "harvestDate", "yieldKg", "unit"));
        config.put("inventoryAttributeKeys", List.of("stockKg", "availableFrom", "shelfLifeDays", "packaging"));
        config.put(
                "deliveryAttributeKeys",
                List.of("deliveryRegion", "deliveryWindow", "carrierRef", "deliveredAt"));

        // Buyer / lead capture (WhatsApp → Lead attributes)
        config.put(
                "leadAttributeKeys",
                List.of(
                        "buyerType",
                        "cropInterest",
                        "volumeKg",
                        "deliveryRegion",
                        "organicRequired",
                        "budget",
                        "harvestWindow"));
        config.put("qualificationPromptCode", "LEAD_QUALIFY_NATURAL_FARMING");
        config.put(
                "qualificationVariableKeys",
                List.of("budget", "cropInterest", "volumeKg", "organicRequired", "deliveryRegion"));
        config.put("recommendationPromptCode", "CATALOG_RECOMMEND_NATURAL_FARMING");

        config.put("conversationFollowupWorkflowKey", "CONVERSATION_FOLLOWUP_V1");
        config.put("defaultFollowupType", "DELIVERY_FOLLOWUP");
        config.put("conversationSubjectTemplate", "Harvest / delivery follow-up");
        config.put("inPersonEngagementLabel", "Farm visit");

        config.put("orderWorkflowKey", "LEAD_LIFECYCLE_V1");
        config.put("paymentCapabilityRef", "billing-service");
        config.put("whatsappLeadCaptureEnabled", true);
        config.put("knowledgeCategoryCode", "NATURAL_FARMING_FAQ");
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
