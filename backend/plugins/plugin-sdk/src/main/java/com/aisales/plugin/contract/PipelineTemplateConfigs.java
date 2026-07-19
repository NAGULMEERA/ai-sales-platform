package com.aisales.plugin.contract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Loads industry pipeline graph metadata from classpath JSON.
 * Authoring source of truth for plugin {@code defaultConfig.pipelineTemplate}.
 */
public final class PipelineTemplateConfigs {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PipelineTemplateConfigs() {
    }

    public static Map<String, Object> load(Class<?> anchor, String resourcePath) {
        ClassLoader loader = anchor.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Missing pipeline template resource: " + resourcePath);
            }
            return MAPPER.readValue(in, new TypeReference<Map<String, Object>>() {});
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load pipeline template: " + resourcePath, ex);
        }
    }
}
