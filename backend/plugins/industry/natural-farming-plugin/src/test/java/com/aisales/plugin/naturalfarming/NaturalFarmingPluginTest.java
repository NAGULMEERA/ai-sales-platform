package com.aisales.plugin.naturalfarming;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.plugin.contract.PluginType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NaturalFarmingPluginTest {

    @Test
    void shouldExposeIndustryMetadataWithoutMicroservice() {
        var plugin = new NaturalFarmingPlugin();
        assertThat(plugin.descriptor().getPluginKey()).isEqualTo("natural-farming");
        assertThat(plugin.descriptor().getType()).isEqualTo(PluginType.INDUSTRY);
        assertThat(plugin.getIndustryCode()).isEqualTo("NATURAL_FARMING");
        assertThat(plugin.descriptor().getMetadata()).containsEntry("ownsMicroservice", false);
        assertThat(plugin.descriptor().getCapabilities())
                .contains(
                        "industry.natural_farming",
                        "catalog.recommend",
                        "notification.whatsapp",
                        "workflow.order",
                        "billing.payment");

        assertThat(plugin.descriptor().getDefaultConfig())
                .containsEntry("defaultPipelineCode", "NATURAL_FARMING_SALES_V1")
                .containsEntry(
                        "pipelineHappyPath",
                        List.of(
                                "New",
                                "Qualified",
                                "Farm Visit",
                                "Negotiation",
                                "Order Confirmed",
                                "Delivered"))
                .containsEntry("catalogOfferCategory", "natural-produce")
                .containsEntry("quoteLineSource", "catalog.offerId")
                .containsEntry("qualificationPromptCode", "LEAD_QUALIFY_NATURAL_FARMING")
                .containsEntry("recommendationPromptCode", "CATALOG_RECOMMEND_NATURAL_FARMING")
                .containsEntry("defaultFollowupType", "DELIVERY_FOLLOWUP")
                .containsEntry("inPersonEngagementLabel", "Farm visit")
                .containsEntry("orderWorkflowKey", "LEAD_LIFECYCLE_V1")
                .containsEntry("whatsappLeadCaptureEnabled", true)
                .containsEntry("knowledgeCategoryCode", "NATURAL_FARMING_FAQ");

        assertThat(plugin.descriptor().getDefaultConfig().get("matchAttributeKeys"))
                .isEqualTo(List.of("cropType", "region", "organicCertified", "season"));
        assertThat(plugin.descriptor().getDefaultConfig().get("leadAttributeKeys"))
                .asList()
                .contains("cropInterest", "volumeKg", "organicRequired");
        assertThat(plugin.descriptor().getDefaultConfig().get("farmAttributeKeys"))
                .asList()
                .contains("farmId", "plotId");
        assertThat(plugin.descriptor().getDefaultConfig().get("harvestAttributeKeys"))
                .asList()
                .contains("harvestDate", "yieldKg");
        assertThat(plugin.descriptor().getDefaultConfig().get("inventoryAttributeKeys"))
                .asList()
                .contains("stockKg");

        @SuppressWarnings("unchecked")
        var template =
                (Map<String, Object>) plugin.descriptor().getDefaultConfig().get("pipelineTemplate");
        assertThat(template).containsEntry("code", "NATURAL_FARMING_SALES_V1");
        assertThat(template.get("stages")).asList().isNotEmpty();
        assertThat(template.get("transitions")).asList().isNotEmpty();

        @SuppressWarnings("unchecked")
        var owners = (Map<String, Object>) plugin.descriptor().getMetadata().get("platformOwners");
        assertThat(owners)
                .containsEntry("productCatalog", "catalog-service")
                .containsEntry("orderManagement", "deal-service (opportunity + quote)")
                .containsEntry("payment", "billing-service");
    }
}
