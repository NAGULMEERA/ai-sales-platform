package com.aisales.common.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailNormalizerTest {

    @Test
    void shouldLowercaseAndTrim() {
        assertThat(EmailNormalizer.normalize("  Admin@Example.COM ")).isEqualTo("admin@example.com");
    }

    @Test
    void shouldPassThroughBlank() {
        assertThat(EmailNormalizer.normalize(null)).isNull();
        assertThat(EmailNormalizer.normalize("")).isEmpty();
    }
}
