package com.aisales.integration.infrastructure.meta;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.integration.application.dto.StubLeadAdsPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class MetaGraphLeadClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMapGraphFieldDataToLeadPayload() throws Exception {
        String json = """
                {
                  "id": "lg_99",
                  "ad_id": "ad_1",
                  "ad_name": "RE Summer",
                  "form_id": "form_1",
                  "platform": "ig",
                  "field_data": [
                    {"name": "full_name", "values": ["Ravi Kumar"]},
                    {"name": "phone_number", "values": ["+919999999999"]},
                    {"name": "email", "values": ["ravi@example.com"]},
                    {"name": "budget", "values": ["80L"]},
                    {"name": "city", "values": ["Bengaluru"]}
                  ]
                }
                """;

        StubLeadAdsPayload payload =
                MetaGraphLeadClient.mapLead(objectMapper.readTree(json), "page_1", "lg_fallback");

        assertThat(payload.getPageId()).isEqualTo("page_1");
        assertThat(payload.getLeadgenId()).isEqualTo("lg_99");
        assertThat(payload.getFullName()).isEqualTo("Ravi Kumar");
        assertThat(payload.getPhone()).isEqualTo("+919999999999");
        assertThat(payload.getEmail()).isEqualTo("ravi@example.com");
        assertThat(payload.getCampaign()).isEqualTo("RE Summer");
        assertThat(payload.getPlatform()).isEqualTo("ig");
        assertThat(payload.getFields())
                .containsEntry("budget", "80L")
                .containsEntry("city", "Bengaluru")
                .containsEntry("metaAdId", "ad_1")
                .containsEntry("metaFormId", "form_1");
    }
}
