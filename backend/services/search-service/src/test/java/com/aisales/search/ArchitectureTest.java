package com.aisales.search;

import com.aisales.common.testing.architecture.LayeredArchitectureRules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

class ArchitectureTest {

    @Test
    @DisabledIfSystemProperty(named = "java.specification.version", matches = "2[6-9]|\\d{3,}")
    void shouldRespectLayeredArchitecture() {
        LayeredArchitectureRules.checkPackage("com.aisales.search");
    }
}
