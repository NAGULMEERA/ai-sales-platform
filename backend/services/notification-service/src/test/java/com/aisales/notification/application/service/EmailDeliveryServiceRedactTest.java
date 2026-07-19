package com.aisales.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailDeliveryServiceRedactTest {

    @Test
    void shouldRedactLocalPart() {
        assertThat(EmailDeliveryService.redactEmail("buyer@example.com")).isEqualTo("***@example.com");
    }

    @Test
    void shouldHandleMissingAt() {
        assertThat(EmailDeliveryService.redactEmail("not-an-email")).isEqualTo("***");
    }
}
