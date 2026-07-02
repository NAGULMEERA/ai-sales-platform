package com.aisales.ai.infrastructure.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(EmbeddingProperties.class)
public class EmbeddingConfiguration {

    /**
     * Shared builder template. Providers must call {@link RestClient.Builder#clone()} before
     * customizing base URL / headers / timeouts per call, since this bean is a singleton and the
     * default builder implementation mutates in place rather than copying on write.
     */
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /**
     * Builds a {@link ClientHttpRequestFactory} with explicit connect/read timeouts so calls to
     * embedding providers (self-hosted or commercial) never block indefinitely.
     */
    public static ClientHttpRequestFactory timeoutRequestFactory(long connectTimeoutMs, long readTimeoutMs) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .withReadTimeout(Duration.ofMillis(readTimeoutMs));
        return ClientHttpRequestFactories.get(settings);
    }
}
