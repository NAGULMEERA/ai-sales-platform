package com.aisales.common.observability.tracing;

import brave.sampler.Sampler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    @Bean
    public Sampler defaultSampler(
            @Value("${aisales.tracing.sample-rate:1.0}") float sampleRate) {
        return Sampler.create(sampleRate);
    }
}
