package com.aisales.plugin.realestate;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.plugin.contract.PluginType;
import org.junit.jupiter.api.Test;

class RealEstatePluginTest {

    @Test
    void shouldExposeIndustryMetadataWithoutMicroservice() {
        var plugin = new RealEstatePlugin();
        assertThat(plugin.descriptor().getPluginKey()).isEqualTo("real-estate");
        assertThat(plugin.descriptor().getType()).isEqualTo(PluginType.INDUSTRY);
        assertThat(plugin.getIndustryCode()).isEqualTo("REAL_ESTATE");
        assertThat(plugin.descriptor().getMetadata()).containsEntry("ownsMicroservice", false);
        assertThat(plugin.descriptor().getDefaultConfig()).containsKey("catalogAttributeKeys");
        assertThat(plugin.descriptor().getDefaultConfig())
                .containsEntry("defaultPipelineCode", "REAL_ESTATE_SALES_V1")
                .containsEntry("pipelineHappyPath",
                        java.util.List.of("New", "Qualified", "Visit", "Negotiation", "Booked"))
                .containsEntry("matchAttributeKeys",
                        java.util.List.of("bedrooms", "bathrooms", "location"))
                .containsEntry("catalogOfferCategory", "residential")
                .containsEntry("quoteLineSource", "catalog.offerId")
                .containsEntry("leadAttributeKeys",
                        java.util.List.of("budget", "location", "propertyType", "timeline"))
                .containsEntry("qualificationPromptCode", "LEAD_QUALIFY_REAL_ESTATE")
                .containsEntry("qualificationVariableKeys",
                        java.util.List.of("budget", "location", "timeline"))
                .containsEntry("conversationFollowupWorkflowKey", "CONVERSATION_FOLLOWUP_V1")
                .containsEntry("defaultFollowupType", "VISIT_FOLLOWUP")
                .containsEntry("conversationSubjectTemplate", "Visit follow-up")
                .containsEntry("inPersonEngagementLabel", "Site visit");
        @SuppressWarnings("unchecked")
        var template = (java.util.Map<String, Object>) plugin.descriptor().getDefaultConfig().get("pipelineTemplate");
        assertThat(template).containsEntry("code", "REAL_ESTATE_SALES_V1");
        assertThat(template.get("stages")).asList().isNotEmpty();
        assertThat(template.get("transitions")).asList().isNotEmpty();
        assertThat(plugin.descriptor().getMetadata())
                .containsEntry("industryQuoteType", false)
                .containsEntry("industryConversationType", false);
    }
}
