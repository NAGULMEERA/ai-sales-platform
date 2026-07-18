package com.aisales.common.contracts.lead;

/**
 * Lead journey stages. Semantic aliases for sales language:
 * {@code APPOINTMENT_BOOKED} ≈ site visit scheduled,
 * {@code VISITED} ≈ site visit completed,
 * {@code WON} ≈ booked / converted.
 */
public enum LeadStatus {
    NEW,
    CONTACTED,
    QUALIFIED,
    APPOINTMENT_BOOKED,
    VISITED,
    NEGOTIATING,
    WON,
    LOST,
    ARCHIVED
}
