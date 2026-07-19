package com.aisales.marketplace.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SemVerTest {

    @Test
    void shouldCompareMajorMinorPatch() {
        assertThat(SemVer.compare("1.0.0", "1.0.0")).isZero();
        assertThat(SemVer.compare("1.2.0", "1.1.9")).isPositive();
        assertThat(SemVer.compare("1.0.0", "2.0.0")).isNegative();
        assertThat(SemVer.isAtLeast("1.0.0", "1.0.0")).isTrue();
        assertThat(SemVer.isAtLeast("1.0.0", "1.0.1")).isFalse();
        assertThat(SemVer.isAtLeast("1.2.3-SNAPSHOT", "1.2.0")).isTrue();
    }
}
