-- Full-text search for keyword/hybrid retrieval + prompt version APPROVED status.

ALTER TABLE knowledge_chunk
    ADD COLUMN IF NOT EXISTS content_tsv tsvector;

UPDATE knowledge_chunk
SET content_tsv = to_tsvector('english', coalesce(content, ''))
WHERE content_tsv IS NULL;

CREATE OR REPLACE FUNCTION knowledge_chunk_tsv_update()
RETURNS TRIGGER AS $$
BEGIN
    NEW.content_tsv := to_tsvector('english', coalesce(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_knowledge_chunk_tsv ON knowledge_chunk;
CREATE TRIGGER trg_knowledge_chunk_tsv
    BEFORE INSERT OR UPDATE OF content ON knowledge_chunk
    FOR EACH ROW EXECUTE FUNCTION knowledge_chunk_tsv_update();

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_tsv
    ON knowledge_chunk USING GIN (content_tsv);

ALTER TABLE prompt_version DROP CONSTRAINT IF EXISTS ck_prompt_version_status;
ALTER TABLE prompt_version
    ADD CONSTRAINT ck_prompt_version_status
    CHECK (status IN ('DRAFT', 'APPROVED', 'ACTIVE', 'ARCHIVED'));
