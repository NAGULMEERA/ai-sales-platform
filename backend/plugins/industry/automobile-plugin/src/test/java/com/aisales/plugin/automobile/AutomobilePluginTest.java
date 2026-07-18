package com.aisales.plugin.automobile;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.plugin.contract.PluginType;
import org.junit.jupiter.api.Test;

class AutomobilePluginTest {

    @Test
    void shouldExposeIndustryMetadataWithoutMicroservice() {
        var plugin = new AutomobilePlugin();
        assertThat(plugin.descriptor().getPluginKey()).isEqualTo("automobile");
        assertThat(plugin.descriptor().getType()).isEqualTo(PluginType.INDUSTRY);
        assertThat(plugin.getIndustryCode()).isEqualTo("AUTOMOBILE");
        assertThat(plugin.descriptor().getMetadata()).containsEntry("ownsMicroservice", false);
        assertThat(plugin.descriptor().getDefaultConfig())
                .containsEntry("defaultPipelineCode", "AUTOMOBILE_SALES_V1")
                .containsEntry("pipelineHappyPath",
                        java.util.List.of("New", "Qualified", "Test Drive", "Quotation", "Finance", "Booked"))
                .containsEntry("matchAttributeKeys",
                        java.util.List.of("make", "model", "year"))
                .containsEntry("catalogOfferCategory", "vehicle")
                .containsEntry("quoteLineSource", "catalog.offerId")
                .containsEntry("leadAttributeKeys",
                        java.util.List.of("vehicle", "budget", "financeRequired", "exchange"))
                .containsEntry("qualificationPromptCode", "LEAD_QUALIFY_AUTOMOBILE")
                .containsEntry("qualificationVariableKeys",
                        java.util.List.of("budget", "vehicle", "financeRequired", "exchange"))
                .containsEntry("conversationFollowupWorkflowKey", "CONVERSATION_FOLLOWUP_V1")
                .containsEntry("defaultFollowupType", "TEST_DRIVE_FOLLOWUP")
                .containsEntry("conversationSubjectTemplate", "Test-drive follow-up")
                .containsEntry("inPersonEngagementLabel", "Test drive");
        assertThat(plugin.descriptor().getMetadata())
                .containsEntry("industryQuoteType", false)
                .containsEntry("industryConversationType", false);
    }
}
