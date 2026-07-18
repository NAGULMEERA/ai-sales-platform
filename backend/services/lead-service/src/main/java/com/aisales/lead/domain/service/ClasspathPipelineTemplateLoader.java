package com.aisales.lead.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Loads industry pipeline graphs from {@code classpath:pipeline-templates/*.json}.
 * Authoring SoT lives in industry plugin resources; lead-service ships the same files for ensure.
 */
@Component
public class ClasspathPipelineTemplateLoader {

    private static final String PATTERN = "classpath:pipeline-templates/*.json";

    private final ObjectMapper objectMapper;

    public ClasspathPipelineTemplateLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<PipelineTemplateDefinition> loadAll() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(PATTERN);
            List<PipelineTemplateDefinition> templates = new ArrayList<>();
            for (Resource resource : resources) {
                if (!resource.isReadable()) {
                    continue;
                }
                templates.add(load(resource));
            }
            return templates;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to scan pipeline template resources", ex);
        }
    }

    public PipelineTemplateDefinition load(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            PipelineTemplateDocument document = objectMapper.readValue(in, PipelineTemplateDocument.class);
            return MetadataPipelineTemplate.fromDocument(document);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Invalid pipeline template metadata: " + resource.getDescription(), ex);
        }
    }
}
