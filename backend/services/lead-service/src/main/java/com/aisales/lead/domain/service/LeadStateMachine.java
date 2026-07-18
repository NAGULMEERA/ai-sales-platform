package com.aisales.lead.domain.service;

import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.exception.exception.ValidationException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LeadStateMachine {

    private static final Map<LeadStatus, Set<LeadStatus>> ALLOWED = new EnumMap<>(LeadStatus.class);

    static {
        ALLOWED.put(LeadStatus.NEW, EnumSet.of(
                LeadStatus.CONTACTED, LeadStatus.QUALIFIED, LeadStatus.LOST));
        ALLOWED.put(LeadStatus.CONTACTED, EnumSet.of(
                LeadStatus.QUALIFIED, LeadStatus.APPOINTMENT_BOOKED, LeadStatus.LOST));
        ALLOWED.put(LeadStatus.QUALIFIED, EnumSet.of(
                LeadStatus.CONTACTED, LeadStatus.APPOINTMENT_BOOKED, LeadStatus.NEGOTIATING, LeadStatus.LOST));
        ALLOWED.put(LeadStatus.APPOINTMENT_BOOKED, EnumSet.of(
                LeadStatus.VISITED, LeadStatus.NEGOTIATING, LeadStatus.LOST));
        ALLOWED.put(LeadStatus.VISITED, EnumSet.of(
                LeadStatus.NEGOTIATING, LeadStatus.WON, LeadStatus.LOST));
        ALLOWED.put(LeadStatus.NEGOTIATING, EnumSet.of(
                LeadStatus.WON, LeadStatus.LOST));
        ALLOWED.put(LeadStatus.WON, EnumSet.noneOf(LeadStatus.class));
        ALLOWED.put(LeadStatus.LOST, EnumSet.noneOf(LeadStatus.class));
    }

    public boolean isTerminal(LeadStatus status) {
        return status == LeadStatus.WON || status == LeadStatus.LOST;
    }

    public void assertTransition(LeadStatus from, LeadStatus to) {
        if (from == to) {
            return;
        }
        Set<LeadStatus> allowed = ALLOWED.getOrDefault(from, EnumSet.noneOf(LeadStatus.class));
        if (!allowed.contains(to)) {
            throw new ValidationException("Invalid lead status transition from " + from + " to " + to);
        }
    }
}
