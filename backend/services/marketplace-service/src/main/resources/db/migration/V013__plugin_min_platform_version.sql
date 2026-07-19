-- Track B: plugin ↔ platform compatibility (required minimum platform version).

ALTER TABLE plugin_catalog
    ADD COLUMN min_platform_version VARCHAR(50) NOT NULL DEFAULT '1.0.0';

COMMENT ON COLUMN plugin_catalog.min_platform_version IS
    'Minimum aisales.platform.version required to enable this plugin (semver major.minor.patch).';
