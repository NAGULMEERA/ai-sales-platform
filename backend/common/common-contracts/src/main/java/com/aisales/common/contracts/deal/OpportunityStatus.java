package com.aisales.common.contracts.deal;

/**
 * Opportunity commercial lifecycle.
 * Created → OPEN → QUALIFIED → QUOTED (proposal) → NEGOTIATION → WON | LOST.
 * CANCELLED is a terminal exit; reopen restores to OPEN or QUALIFIED.
 */
public enum OpportunityStatus {
    OPEN,
    QUALIFIED,
    QUOTED,
    NEGOTIATION,
    WON,
    LOST,
    CANCELLED
}
