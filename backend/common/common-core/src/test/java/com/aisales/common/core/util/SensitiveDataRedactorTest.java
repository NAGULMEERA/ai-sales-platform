package com.aisales.common.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveDataRedactorTest {

    @Test
    void shouldRedactEmailLocalPart() {
        assertThat(SensitiveDataRedactor.redactEmail("buyer@example.com")).isEqualTo("***@example.com");
        assertThat(SensitiveDataRedactor.redactEmail("not-an-email")).isEqualTo("***");
        assertThat(SensitiveDataRedactor.redactEmail(null)).isEqualTo("(none)");
    }

    @Test
    void shouldRedactTokenAndPhone() {
        assertThat(SensitiveDataRedactor.redactToken("abcdef123456")).isEqualTo("****3456");
        assertThat(SensitiveDataRedactor.redactPhone("+91-98765-43210")).isEqualTo("***3210");
    }
}
