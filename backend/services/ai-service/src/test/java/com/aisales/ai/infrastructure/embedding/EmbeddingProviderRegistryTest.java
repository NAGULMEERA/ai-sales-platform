package com.aisales.ai.infrastructure.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.domain.embedding.EmbeddingProviderKind;
import com.aisales.ai.infrastructure.configuration.EmbeddingProperties;
import com.aisales.common.exception.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmbeddingProviderRegistryTest {

    @Test
    void shouldResolveDefaultByProviderFlag() {
        EmbeddingProperties properties = new EmbeddingProperties();
        properties.setProvider("STUB");
        EmbeddingProviderRegistry registry =
                new EmbeddingProviderRegistry(List.of(new StubEmbeddingProvider()), properties);

        assertThat(registry.resolveDefault().name()).isEqualTo("STUB");
    }

    @Test
    void shouldSwitchProviderByFlagOnly() {
        EmbeddingProperties properties = new EmbeddingProperties();
        properties.setProvider("TEI");
        EmbeddingProvider tei = new FixedNameProvider("TEI", EmbeddingProviderKind.OPEN_SOURCE, "BAAI/bge-m3");
        EmbeddingProvider openAi = new FixedNameProvider("OPENAI", EmbeddingProviderKind.COMMERCIAL, "text-embedding-3-small");
        EmbeddingProviderRegistry registry =
                new EmbeddingProviderRegistry(List.of(new StubEmbeddingProvider(), tei, openAi), properties);

        assertThat(registry.resolveDefault().name()).isEqualTo("TEI");

        properties.setProvider("OPENAI");
        assertThat(registry.resolveDefault().name()).isEqualTo("OPENAI");
    }

    @Test
    void shouldFailWhenProviderBeanMissing() {
        EmbeddingProperties properties = new EmbeddingProperties();
        properties.setProvider("OPENAI");
        EmbeddingProviderRegistry registry =
                new EmbeddingProviderRegistry(List.of(new StubEmbeddingProvider()), properties);

        assertThatThrownBy(registry::resolveDefault)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OPENAI");
    }

    private static final class FixedNameProvider implements EmbeddingProvider {
        private final String name;
        private final EmbeddingProviderKind kind;
        private final String model;

        private FixedNameProvider(String name, EmbeddingProviderKind kind, String model) {
            this.name = name;
            this.kind = kind;
            this.model = model;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public EmbeddingProviderKind kind() {
            return kind;
        }

        @Override
        public String modelName() {
            return model;
        }

        @Override
        public int dimension() {
            return 1024;
        }

        @Override
        public boolean supports(String modelName) {
            return model.equals(modelName);
        }

        @Override
        public List<float[]> embed(List<String> texts) {
            return List.of();
        }
    }
}
