package com.aisales.common.contracts.ai;

public enum PromptStatus {
    DRAFT,
    /** Reviewed and eligible for activation. */
    APPROVED,
    ACTIVE,
    ARCHIVED
}
