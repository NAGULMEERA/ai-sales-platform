-- PluginInstallationRepository.findByTenantIdOrderByUpdatedAtDesc (PluginRegistryService.listInstallations)
-- filters by tenant_id and sorts by updated_at DESC. The existing idx_plugin_installation_tenant_status
-- (tenant_id, status) index (V002__plugin_registry.sql) does not cover the ORDER BY, forcing an
-- extra sort pass on every call.
CREATE INDEX IF NOT EXISTS idx_plugin_installation_tenant_updated
    ON plugin_installation (tenant_id, updated_at DESC);
