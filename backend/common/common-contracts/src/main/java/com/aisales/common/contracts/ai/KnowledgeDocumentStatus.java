package com.aisales.common.contracts.ai;

public enum KnowledgeDocumentStatus {
    PENDING,
    /** Claimed by an in-flight indexer; concurrent index requests are rejected. */
    INDEXING,
    READY,
    FAILED,
    ARCHIVED
}
