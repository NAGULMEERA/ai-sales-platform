package com.aisales.lead.domain.service;

import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.exception.exception.ValidationException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pipeline template materialized from plugin/classpath JSON metadata.
 */
public final class MetadataPipelineTemplate implements PipelineTemplateDefinition {

    private final String code;
    private final String name;
    private final String description;
    private final List<StageSeed> stages;
    private final Map<LeadStatus, Set<LeadStatus>> transitions;

    public MetadataPipelineTemplate(
            String code,
            String name,
            String description,
            List<StageSeed> stages,
            Map<LeadStatus, Set<LeadStatus>> transitions) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.stages = List.copyOf(stages);
        Map<LeadStatus, Set<LeadStatus>> copy = new EnumMap<>(LeadStatus.class);
        transitions.forEach((from, to) -> copy.put(from, EnumSet.copyOf(to)));
        this.transitions = Map.copyOf(copy);
    }

    public static MetadataPipelineTemplate fromDocument(PipelineTemplateDocument document) {
        if (document == null || document.code() == null || document.code().isBlank()) {
            throw new ValidationException("Pipeline template code is required in metadata");
        }
        if (document.stages() == null || document.stages().isEmpty()) {
            throw new ValidationException("Pipeline template stages are required: " + document.code());
        }
        List<StageSeed> stages = new ArrayList<>();
        for (PipelineTemplateDocument.StageDocument stage : document.stages()) {
            stages.add(new StageSeed(
                    parseStatus(stage.status()),
                    stage.displayName(),
                    stage.order(),
                    stage.terminal()));
        }
        Map<LeadStatus, Set<LeadStatus>> transitions = new EnumMap<>(LeadStatus.class);
        if (document.transitions() != null) {
            for (PipelineTemplateDocument.TransitionDocument edge : document.transitions()) {
                LeadStatus from = parseStatus(edge.from());
                EnumSet<LeadStatus> to = EnumSet.noneOf(LeadStatus.class);
                if (edge.to() != null) {
                    for (String target : edge.to()) {
                        to.add(parseStatus(target));
                    }
                }
                transitions.put(from, to);
            }
        }
        return new MetadataPipelineTemplate(
                document.code().trim().toUpperCase(),
                document.name() != null ? document.name() : document.code(),
                document.description() != null ? document.description() : "",
                stages,
                transitions);
    }

    private static LeadStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Pipeline stage/transition status is required");
        }
        try {
            return LeadStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Unknown LeadStatus in pipeline metadata: " + raw);
        }
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public List<StageSeed> stages() {
        return stages;
    }

    @Override
    public Map<LeadStatus, Set<LeadStatus>> transitions() {
        Map<LeadStatus, Set<LeadStatus>> copy = new EnumMap<>(LeadStatus.class);
        transitions.forEach((from, to) -> copy.put(from, EnumSet.copyOf(to)));
        return copy;
    }

    public Set<LeadStatus> allowedTargets(LeadStatus from) {
        return EnumSet.copyOf(transitions.getOrDefault(from, EnumSet.noneOf(LeadStatus.class)));
    }
}
