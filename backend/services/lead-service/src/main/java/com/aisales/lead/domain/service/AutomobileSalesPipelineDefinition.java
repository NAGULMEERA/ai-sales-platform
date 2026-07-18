package com.aisales.lead.domain.service;

import com.aisales.common.contracts.lead.LeadStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Automobile sales path on the shared pipeline engine.
 * Proof path: New → Qualified → Test Drive → Quotation → Finance → Booked.
 */
public final class AutomobileSalesPipelineDefinition implements PipelineTemplateDefinition {

    public static final String CODE = "AUTOMOBILE_SALES_V1";
    public static final AutomobileSalesPipelineDefinition INSTANCE = new AutomobileSalesPipelineDefinition();

    private static final Map<LeadStatus, Set<LeadStatus>> TRANSITIONS = new EnumMap<>(LeadStatus.class);

    static {
        TRANSITIONS.put(LeadStatus.NEW, EnumSet.of(
                LeadStatus.CONTACTED, LeadStatus.QUALIFIED, LeadStatus.LOST));
        TRANSITIONS.put(LeadStatus.CONTACTED, EnumSet.of(
                LeadStatus.QUALIFIED, LeadStatus.LOST));
        TRANSITIONS.put(LeadStatus.QUALIFIED, EnumSet.of(
                LeadStatus.APPOINTMENT_BOOKED, LeadStatus.LOST));
        TRANSITIONS.put(LeadStatus.APPOINTMENT_BOOKED, EnumSet.of(
                LeadStatus.VISITED, LeadStatus.LOST));
        TRANSITIONS.put(LeadStatus.VISITED, EnumSet.of(
                LeadStatus.NEGOTIATING, LeadStatus.LOST));
        TRANSITIONS.put(LeadStatus.NEGOTIATING, EnumSet.of(
                LeadStatus.WON, LeadStatus.LOST));
        TRANSITIONS.put(LeadStatus.WON, EnumSet.of(LeadStatus.ARCHIVED));
        TRANSITIONS.put(LeadStatus.LOST, EnumSet.of(LeadStatus.ARCHIVED));
        TRANSITIONS.put(LeadStatus.ARCHIVED, EnumSet.noneOf(LeadStatus.class));
    }

    private AutomobileSalesPipelineDefinition() {
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String name() {
        return "Automobile Sales Pipeline";
    }

    @Override
    public String description() {
        return "Automobile journey: New → Qualified → Test Drive → Quotation → Finance → Booked. Stage codes remain LeadStatus.";
    }

    @Override
    public List<StageSeed> stages() {
        return List.of(
                new StageSeed(LeadStatus.NEW, "New", 10, false),
                new StageSeed(LeadStatus.CONTACTED, "Contacted", 20, false),
                new StageSeed(LeadStatus.QUALIFIED, "Qualified", 30, false),
                new StageSeed(LeadStatus.APPOINTMENT_BOOKED, "Test Drive", 40, false),
                new StageSeed(LeadStatus.VISITED, "Quotation", 50, false),
                new StageSeed(LeadStatus.NEGOTIATING, "Finance", 60, false),
                new StageSeed(LeadStatus.WON, "Booked", 70, true),
                new StageSeed(LeadStatus.LOST, "Lost", 80, true),
                new StageSeed(LeadStatus.ARCHIVED, "Archived", 90, true)
        );
    }

    @Override
    public Map<LeadStatus, Set<LeadStatus>> transitions() {
        Map<LeadStatus, Set<LeadStatus>> copy = new EnumMap<>(LeadStatus.class);
        TRANSITIONS.forEach((from, to) -> copy.put(from, EnumSet.copyOf(to)));
        return copy;
    }

    public static Set<LeadStatus> allowedTargets(LeadStatus from) {
        return EnumSet.copyOf(TRANSITIONS.getOrDefault(from, EnumSet.noneOf(LeadStatus.class)));
    }
}
