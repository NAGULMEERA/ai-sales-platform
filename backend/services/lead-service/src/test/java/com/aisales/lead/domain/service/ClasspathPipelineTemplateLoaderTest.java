package com.aisales.lead.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.common.contracts.lead.LeadStatus;
import org.junit.jupiter.api.Test;

class ClasspathPipelineTemplateLoaderTest {

    @Test
    void shouldLoadIndustryTemplatesFromClasspathJson() {
        PipelineTemplateRegistry registry = PipelineTemplateTestSupport.registry();

        assertThat(registry.codes())
                .contains("DEFAULT_SALES_V1", "REAL_ESTATE_SALES_V1", "AUTOMOBILE_SALES_V1");

        PipelineTemplateDefinition re = registry.require("REAL_ESTATE_SALES_V1");
        assertThat(re.stages()).anySatisfy(s -> {
            assertThat(s.status()).isEqualTo(LeadStatus.VISITED);
            assertThat(s.displayName()).isEqualTo("Visit");
        });
        assertThat(re.transitions().get(LeadStatus.QUALIFIED))
                .contains(LeadStatus.VISITED)
                .doesNotContain(LeadStatus.APPOINTMENT_BOOKED);

        PipelineTemplateDefinition auto = registry.require("AUTOMOBILE_SALES_V1");
        assertThat(auto.transitions().get(LeadStatus.QUALIFIED))
                .contains(LeadStatus.APPOINTMENT_BOOKED)
                .doesNotContain(LeadStatus.VISITED);
    }
}
