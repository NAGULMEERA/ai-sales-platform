-- Object storage metadata. Binary bytes live in LOCAL filesystem or S3 (never in PostgreSQL).

ALTER TABLE media_metadata
    ADD COLUMN original_filename VARCHAR(500),
    ADD COLUMN content_type VARCHAR(150),
    ADD COLUMN size_bytes BIGINT,
    ADD COLUMN checksum_sha256 VARCHAR(64),
    ADD COLUMN bucket_name VARCHAR(255),
    ADD COLUMN object_key VARCHAR(1000),
    ADD COLUMN storage_provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED';

CREATE INDEX idx_media_metadata_tenant_status
    ON media_metadata (tenant_id, status)
    WHERE deleted_at IS NULL;

COMMENT ON COLUMN media_metadata.object_key IS 'Storage object key; binary resides outside PostgreSQL';
COMMENT ON COLUMN media_metadata.storage_provider IS 'LOCAL or S3 (aisales.media.storage plug/flag)';
