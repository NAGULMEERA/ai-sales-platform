package com.aisales.lead.domain.service;

import com.aisales.common.contracts.lead.LeadStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bootstrap graph for the default sales pipeline ({@code DEFAULT_SALES_V1}).
 * Matches the historical hardcoded {@link LeadStateMachine} transitions.
 */
public final class DefaultSalesPipelineDefinition {

    public static final String CODE = "DEFAULT_SALES_V1";
    public static final String NAME = "Default Sales Pipeline";
    public static final String DESCRIPTION =
            "Platform default sales journey. Stage codes align with LeadStatus.";

    private static final Map<LeadStatus, Set<LeadStatus>> TRANSITIONS = new EnumMap<>(LeadStatus.class);

    static {
        TRANSITIONS.put(LeadStatus.NEW, EnumSet.of(
                LeadStatus.CONTACTED, LeadStatus.QUALIFIED, LeadStatus.LOST));
        TRANSITIONS.put(LeadStatus.CONTACTED, EnumSet.of(
                LeadStatus.QUALIFIED, LeadStatus.APPOINTMENT_BOOKED, LeadStatus.LOST));
        TRANSITIONS.put(LeadStatus.QUALIFIED, EnumSet.of(
                LeadStatus.CONTACTED, LeadStatus.APPOINTMENT_BOOKED, LeadStatus.NEGOTIATING, LeadStatus.LOST));
        TRANSITIONS.put(LeadStatus.APPOINTMENT_BOOKED, EnumSet.of(
                LeadStatus.VISITED, LeadStatus.QUALIFIED, LeadStatus.NEGOTIATING, LeadStatus.LOST));
        TRANSITIONS.put(LeadStatus.VISITED, EnumSet.of(
                LeadStatus.NEGOTIATING, LeadStatus.WON, LeadStatus.LOST));
        TRANSITIONS.put(LeadStatus.NEGOTIATING, EnumSet.of(
                LeadStatus.WON, LeadStatus.LOST));
        TRANSITIONS.put(LeadStatus.WON, EnumSet.of(LeadStatus.ARCHIVED));
        // Reopen lost leads into an active stage; archive remains available.
        TRANSITIONS.put(LeadStatus.LOST, EnumSet.of(
                LeadStatus.QUALIFIED, LeadStatus.CONTACTED, LeadStatus.ARCHIVED));
        TRANSITIONS.put(LeadStatus.ARCHIVED, EnumSet.noneOf(LeadStatus.class));
    }

    private DefaultSalesPipelineDefinition() {
    }

    public static Map<LeadStatus, Set<LeadStatus>> transitions() {
        Map<LeadStatus, Set<LeadStatus>> copy = new EnumMap<>(LeadStatus.class);
        TRANSITIONS.forEach((from, to) -> copy.put(from, EnumSet.copyOf(to)));
        return copy;
    }

    public static Set<LeadStatus> allowedTargets(LeadStatus from) {
        return EnumSet.copyOf(TRANSITIONS.getOrDefault(from, EnumSet.noneOf(LeadStatus.class)));
    }

    public static List<StageSeed> stages() {
        return List.of(
                new StageSeed(LeadStatus.NEW, "New", 10, false),
                new StageSeed(LeadStatus.CONTACTED, "Contacted", 20, false),
                new StageSeed(LeadStatus.QUALIFIED, "Qualified", 30, false),
                new StageSeed(LeadStatus.APPOINTMENT_BOOKED, "Appointment booked", 40, false),
                new StageSeed(LeadStatus.VISITED, "Visited", 50, false),
                new StageSeed(LeadStatus.NEGOTIATING, "Negotiating", 60, false),
                new StageSeed(LeadStatus.WON, "Won", 70, true),
                new StageSeed(LeadStatus.LOST, "Lost", 80, true),
                new StageSeed(LeadStatus.ARCHIVED, "Archived", 90, true)
        );
    }

    public record StageSeed(LeadStatus status, String displayName, int order, boolean terminal) {
    }
}
