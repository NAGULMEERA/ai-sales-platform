package com.aisales.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.common.exception.exception.ValidationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PromptRendererTest {

    private final PromptRenderer renderer = new PromptRenderer();

    @Test
    void shouldRenderVariables() {
        String rendered = renderer.render(
                "Hello {{name}}, score={{score}}",
                Map.of("name", "Ada", "score", "90"),
                List.of("name", "score"));
        assertThat(rendered).isEqualTo("Hello Ada, score=90");
    }

    @Test
    void shouldRejectMissingDeclaredVariables() {
        assertThatThrownBy(() -> renderer.render(
                        "Hello {{name}}",
                        Map.of(),
                        List.of("name")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Missing prompt variables");
    }

    @Test
    void shouldRejectUnresolvedPlaceholders() {
        assertThatThrownBy(() -> renderer.render(
                        "Hello {{name}}",
                        Map.of("other", "x"),
                        List.of()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unresolved prompt variable");
    }
}
