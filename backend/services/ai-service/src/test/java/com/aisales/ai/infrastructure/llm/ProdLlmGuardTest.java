package com.aisales.ai.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class ProdLlmGuardTest {

    @Test
    void shouldFailWhenStubConfiguredInProd() {
        ProdLlmGuard guard = new ProdLlmGuard("STUB");
        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STUB is forbidden");
    }

    @Test
    void shouldAllowNonStubProvider() {
        ProdLlmGuard guard = new ProdLlmGuard("OPENAI");
        assertThatCode(() -> guard.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowGeminiInProd() {
        ProdLlmGuard guard = new ProdLlmGuard("GEMINI");
        assertThatCode(() -> guard.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }
}
