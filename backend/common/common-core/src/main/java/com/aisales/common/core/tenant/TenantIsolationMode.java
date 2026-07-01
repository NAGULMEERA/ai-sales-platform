package com.aisales.common.core.tenant;

/**
 * Tenant data isolation tier. Default platform mode is SHARED_SCHEMA with RLS.
 */
public enum TenantIsolationMode {

    /** Shared database, shared schema, {@code tenant_id} column + RLS (default). */
    SHARED_SCHEMA,

    /** Shared database, dedicated PostgreSQL schema per tenant (enterprise option). */
    DEDICATED_SCHEMA,

    /** Dedicated database instance per tenant (premium / regulated). */
    DEDICATED_DATABASE
}
