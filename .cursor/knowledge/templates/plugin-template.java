package com.company.platform.template.plugin;

import java.util.Map;
import java.util.UUID;

/**
 * Production-ready Plugin Template.
 *
 * Plugins extend platform capabilities without modifying the core.
 */
public final class RealEstatePlugin implements PlatformPlugin {

    private final PropertySearchService propertySearchService;
    private final WorkflowExtension workflowExtension;

    public RealEstatePlugin(PropertySearchService propertySearchService,
                            WorkflowExtension workflowExtension) {
        this.propertySearchService = propertySearchService;
        this.workflowExtension = workflowExtension;
    }

    @Override
    public PluginMetadata metadata() {
        return new PluginMetadata(
                "real-estate",
                "Real Estate Plugin",
                "1.0.0",
                "Real estate sales capabilities");
    }

    @Override
    public void register(PluginRegistry registry) {
        registry.registerTool("property-search", propertySearchService);
        registry.registerWorkflow("lead-qualification", workflowExtension);
    }

    @Override
    public void validateConfiguration(Map<String, String> configuration) {
        require(configuration, "property.api.url");
        require(configuration, "property.api.key");
    }

    @Override
    public void start() {
        // Initialize connections/resources
    }

    @Override
    public void stop() {
        // Release resources
    }

    private void require(Map<String, String> config, String key) {
        if (!config.containsKey(key) || config.get(key).isBlank()) {
            throw new IllegalArgumentException("Missing configuration: " + key);
        }
    }
}

/* ---------- Plugin SPI ---------- */

interface PlatformPlugin {
    PluginMetadata metadata();
    void register(PluginRegistry registry);
    void validateConfiguration(Map<String,String> configuration);
    void start();
    void stop();
}

record PluginMetadata(
        String id,
        String name,
        String version,
        String description) {}

interface PluginRegistry {
    void registerTool(String name, Object tool);
    void registerWorkflow(String name, WorkflowExtension workflow);
}

/* ---------- Extension Points ---------- */

interface WorkflowExtension {
    WorkflowResult execute(WorkflowContext context);
}

record WorkflowContext(
        UUID tenantId,
        UUID correlationId,
        Map<String,Object> variables) {}

record WorkflowResult(
        boolean success,
        Map<String,Object> outputs) {}

interface PropertySearchService {
    PropertySearchResult search(PropertySearchRequest request);
}

record PropertySearchRequest(
        String location,
        double budget) {}

record PropertySearchResult(
        int totalMatches,
        String summary) {}
