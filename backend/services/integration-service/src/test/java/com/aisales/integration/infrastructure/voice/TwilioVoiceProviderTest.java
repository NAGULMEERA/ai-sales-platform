package com.aisales.integration.infrastructure.voice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TwilioVoiceProviderTest {

    @Test
    void shouldBuildTwimlWithEscapedName() {
        String twiml = TwilioVoiceProvider.buildQualificationTwiml("Ada & Co <test>");
        assertThat(twiml).contains("Ada &amp; Co &lt;test&gt;");
        assertThat(twiml).contains("<Say");
        assertThat(twiml).contains("location, budget");
    }
}
