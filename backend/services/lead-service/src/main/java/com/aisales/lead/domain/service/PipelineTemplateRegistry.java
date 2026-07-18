package com.aisales.lead.domain.service;

import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.exception.exception.ValidationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Known pipeline bootstrap templates.
 * Industry graphs load from classpath JSON (plugin metadata SoT); DEFAULT remains platform Java.
 */
@Component
public class PipelineTemplateRegistry {

    private final Map<String, PipelineTemplateDefinition> templates = new LinkedHashMap<>();

    public PipelineTemplateRegistry(ClasspathPipelineTemplateLoader metadataLoader) {
        register(new DefaultPipelineTemplateAdapter());
        for (PipelineTemplateDefinition template : metadataLoader.loadAll()) {
            register(template);
        }
    }

    public PipelineTemplateDefinition require(String code) {
        if (code == null || code.isBlank()) {
            throw new ValidationException("Pipeline template code is required");
        }
        PipelineTemplateDefinition template = templates.get(code.trim().toUpperCase());
        if (template == null) {
            throw new ValidationException("Unknown pipeline template: " + code);
        }
        return template;
    }

    public Set<String> codes() {
        return Set.copyOf(templates.keySet());
    }

    private void register(PipelineTemplateDefinition template) {
        templates.put(template.code().toUpperCase(), template);
    }

    /** Adapts legacy {@link DefaultSalesPipelineDefinition} to the template interface. */
    private static final class DefaultPipelineTemplateAdapter implements PipelineTemplateDefinition {
        @Override
        public String code() {
            return DefaultSalesPipelineDefinition.CODE;
        }

        @Override
        public String name() {
            return DefaultSalesPipelineDefinition.NAME;
        }

        @Override
        public String description() {
            return DefaultSalesPipelineDefinition.DESCRIPTION;
        }

        @Override
        public List<StageSeed> stages() {
            return DefaultSalesPipelineDefinition.stages().stream()
                    .map(s -> new StageSeed(s.status(), s.displayName(), s.order(), s.terminal()))
                    .toList();
        }

        @Override
        public Map<LeadStatus, Set<LeadStatus>> transitions() {
            return DefaultSalesPipelineDefinition.transitions();
        }
    }
}
