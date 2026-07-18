package com.aisales.lead.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.common.exception.exception.ValidationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LeadQualificationVariableMapperTest {

    private final LeadQualificationVariableMapper mapper = new LeadQualificationVariableMapper();

    @Test
    void shouldMapRealEstateQualificationVariables() {
        Map<String, String> vars = mapper.toVariables(
                "RE Buyer",
                Map.of("budget", 7500000, "location", "Whitefield", "timeline", "3 months",
                        "propertyType", "Apartment"),
                List.of("budget", "location", "timeline"));

        assertThat(vars)
                .containsEntry("leadName", "RE Buyer")
                .containsEntry("budget", "7500000")
                .containsEntry("location", "Whitefield")
                .containsEntry("timeline", "3 months")
                .doesNotContainKey("propertyType");
    }

    @Test
    void shouldMapAutomobileQualificationVariables() {
        Map<String, String> vars = mapper.toVariables(
                "Auto Buyer",
                Map.of("budget", 1800000, "vehicle", "SUV", "financeRequired", true, "exchange", "yes"),
                List.of("budget", "vehicle", "financeRequired", "exchange"));

        assertThat(vars)
                .containsEntry("leadName", "Auto Buyer")
                .containsEntry("vehicle", "SUV")
                .containsEntry("financeRequired", "true")
                .containsEntry("exchange", "yes");
    }

    @Test
    void shouldRejectMissingAttributeKey() {
        assertThatThrownBy(() -> mapper.toVariables(
                "Buyer",
                Map.of("budget", 100),
                List.of("budget", "location")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("location");
    }
}
