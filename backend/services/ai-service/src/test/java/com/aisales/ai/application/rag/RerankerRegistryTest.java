package com.aisales.ai.application.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.ai.infrastructure.configuration.RagProperties;
import com.aisales.common.exception.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class RerankerRegistryTest {

    @Test
    void shouldResolveStubByDefaultFlag() {
        RagProperties properties = new RagProperties();
        properties.setReranker("STUB");
        RerankerRegistry registry =
                new RerankerRegistry(List.of(new NoneReranker(), new StubReranker()), properties);

        assertThat(registry.resolveDefault().name()).isEqualTo("STUB");
    }

    @Test
    void shouldSwitchToNone() {
        RagProperties properties = new RagProperties();
        properties.setReranker("NONE");
        RerankerRegistry registry =
                new RerankerRegistry(List.of(new NoneReranker(), new StubReranker()), properties);

        assertThat(registry.resolveDefault().name()).isEqualTo("NONE");
    }

    @Test
    void shouldFailWhenTeiNotRegistered() {
        RagProperties properties = new RagProperties();
        properties.setReranker("TEI");
        RerankerRegistry registry =
                new RerankerRegistry(List.of(new NoneReranker(), new StubReranker()), properties);

        assertThatThrownBy(registry::resolveDefault)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TEI");
    }
}
