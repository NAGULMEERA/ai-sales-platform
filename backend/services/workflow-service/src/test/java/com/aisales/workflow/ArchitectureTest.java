package com.aisales.workflow;

import com.aisales.common.testing.architecture.LayeredArchitectureRules;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    @Test
    void shouldRespectLayeredArchitecture() {
        LayeredArchitectureRules.checkPackage("com.aisales.workflow");
    }
}
